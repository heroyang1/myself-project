#!/usr/bin/env bash
# ============================================================
# code-quality-guard.sh — 通用化编译前静态质量门禁 v2
#
# 自动检测所有业务实体的字段一致性问题，而非硬编码少数实体。
# 配置文件: .quality-guard.conf
#
# 用法:
#   本地: bash scripts/code-quality-guard.sh
#   CI:   由 pr-check.yml 自动调用（BASE_REF=origin/<target-branch>）
# ============================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

errors=0; warnings=0

err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; ((errors++))   || true; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"  >&2; ((warnings++)) || true; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
info() { echo -e "${CYAN}[INFO]${NC} $*"; }

# ============================================================
# 配置加载
# ============================================================
CONF_FILE=".quality-guard.conf"

# 默认值
REQUIRED_FIELDS="no"
TYPE_CHECK_FIELDS=""
ENUM_FILES="backstage-common/src/main/java/com/backstage/common/enums/ResourceCodePrefixEnum.java"
SERVICE_DIR="backstage-system"
ENTITY_SEARCH_DIR="backstage-system"
DDL_DIR="sql"

if [[ -f "$CONF_FILE" ]]; then
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    case "$key" in
      REQUIRED_FIELDS)  REQUIRED_FIELDS="$value" ;;
      TYPE_CHECK_FIELDS) TYPE_CHECK_FIELDS="$value" ;;
      ENUM_FILES)       ENUM_FILES="$value" ;;
      SERVICE_DIR)      SERVICE_DIR="$value" ;;
      ENTITY_SEARCH_DIR) ENTITY_SEARCH_DIR="$value" ;;
      DDL_DIR)          DDL_DIR="$value" ;;
    esac
  done < "$CONF_FILE"
fi

IFS='|' read -ra REQ_FIELDS_ARR <<< "$REQUIRED_FIELDS"
IFS='|' read -ra TYPE_FIELDS_ARR <<< "$TYPE_CHECK_FIELDS"
IFS='|' read -ra ENUM_FILES_ARR <<< "$ENUM_FILES"

# ── Java→SQL 类型映射表（内置，极少变更）──
JAVA_TYPES=("String" "Long" "Integer" "Boolean" "Date" "BigDecimal" "Double" "Float" "Byte")
SQL_PATTERNS=(
  "varchar|char|text|longtext|mediumtext|tinytext|enum|json"
  "bigint"
  "int|tinyint|smallint|mediumint"
  "tinyint|bit|boolean"
  "datetime|timestamp|date|time"
  "decimal|numeric"
  "double|decimal"
  "float"
  "tinyint|smallint"
)

# ============================================================
# 变更文件检测
# ============================================================
base_ref="${BASE_REF:-HEAD~1}"
if git rev-parse --verify "$base_ref" >/dev/null 2>&1; then
  changed_files="$(git diff --name-only "$base_ref"...HEAD 2>/dev/null || true)"
else
  changed_files="$(git diff --name-only HEAD~1...HEAD 2>/dev/null || true)"
fi
untracked_files="$(git ls-files --others --exclude-standard 2>/dev/null || true)"
changed_files="$(printf '%s\n%s\n' "$changed_files" "$untracked_files" | sed '/^$/d' | sort -u)"

if [[ -z "$changed_files" ]]; then
  ok "No changed files detected, skipping."
  exit 0
fi

echo "======================================"
echo " Code Quality Guard v2 (Generic)"
echo "======================================"
info "Config: ${CONF_FILE}  |  Required fields: ${REQUIRED_FIELDS}"
echo ""

# ============================================================
# 阶段 0: 动态实体发现
# 扫描 ServiceImpl 中的 mapper.insert/insertXxx 调用，
# 自动解析涉及的实体类型，无需硬编码。
# ============================================================
echo "--- Phase 0: Dynamic Entity Discovery ---"

declare -A ENT_CLASS_TO_FILE=()   # EntityClass → .java 路径
declare -A ENT_CLASS_TO_SVC=()    # EntityClass → ServiceImpl 路径
declare -A ENT_CLASS_TO_TABLE=()  # EntityClass → 推测表名

# 1) 收集所有含 insert 调用的 ServiceImpl
insert_services=$(grep -rl --include="*ServiceImpl.java" -E '\.(insert|insert[A-Z])' "$SERVICE_DIR" 2>/dev/null || true)

discovered=0
if [[ -n "$insert_services" ]]; then
  while IFS= read -r svc_file; do
    # 2) 提取该文件中所有 Mapper 字段声明: private XxxMapper varName;
    while IFS= read -r decl; do
      mapper_cls=$(echo "$decl" | sed 's/.*private[[:space:]]\+\([A-Za-z0-9_]*Mapper\)[[:space:]]\+.*/\1/')
      var_name=$(echo "$decl"  | sed 's/.*private[[:space:]]\+[A-Za-z0-9_]*Mapper[[:space:]]\+\([a-zA-Z0-9_]*\)[[:space:]]*;.*$/\1/')

      # 3) 仅当该 mapper 在此文件中有 .insert 调用时才纳入
      if ! grep -qE "${var_name}\.(insert|insert[A-Z])" "$svc_file" 2>/dev/null; then
        continue
      fi

      # 4) 从 Mapper 类名推断实体类名（去掉 Mapper 后缀）
      entity_cls="${mapper_cls%Mapper}"

      # 已发现则跳过
      [[ -n "${ENT_CLASS_TO_FILE[$entity_cls]+x}" ]] && continue

      # 5) 搜索实体类文件
      efile=$(find "$ENTITY_SEARCH_DIR" -name "${entity_cls}.java" -path "*/domain/*" -type f 2>/dev/null | head -1 || true)
      [[ -z "$efile" ]] && continue

      # 6) PascalCase → snake_case 推测表名
      table=$(echo "$entity_cls" | sed 's/\([A-Z]\)/_\L\1/g' | sed 's/^_//')

      ENT_CLASS_TO_FILE[$entity_cls]="$efile"
      ENT_CLASS_TO_SVC[$entity_cls]="$svc_file"
      ENT_CLASS_TO_TABLE[$entity_cls]="$table"
      ((discovered++)) || true
    done < <(grep -E 'private[[:space:]]+[A-Za-z0-9_]*Mapper[[:space:]]+' "$svc_file" 2>/dev/null || true)
  done <<< "$insert_services"
fi

info "Discovered $discovered entities with insert operations"
for ec in "${!ENT_CLASS_TO_FILE[@]}"; do
  info "  $ec → table: ${ENT_CLASS_TO_TABLE[$ec]}"
done
echo ""

# ============================================================
# CHECK 1: 必填字段 Service 层完整性（通用）
# 对每个 discovered 实体，检查配置中的 REQUIRED_FIELDS:
#   - 实体有该字段 → Service insert 路径必须调用 setXxx()
# ============================================================
echo "--- CHECK 1: Required field — Service layer completeness ---"
c1_err=0

for entity in "${!ENT_CLASS_TO_FILE[@]}"; do
  efile="${ENT_CLASS_TO_FILE[$entity]}"
  sfile="${ENT_CLASS_TO_SVC[$entity]}"

  for field in "${REQ_FIELDS_ARR[@]}"; do
    [[ -z "$field" ]] && continue
    # 实体是否声明了该字段
    if grep -q "private[[:space:]].*[[:space:]]${field}[[:space:]]*;" "$efile" 2>/dev/null; then
      setter="set$(echo "${field:0:1}" | tr '[:lower:]' '[:upper:]')${field:1}"
      if ! grep -q "\.${setter}(" "$sfile" 2>/dev/null; then
        err "$(basename "$sfile"): inserts ${entity} but missing ${setter}() call"
        ((c1_err++)) || true
      fi
    fi
  done
done

[[ $c1_err -eq 0 ]] && ok "CHECK 1 passed"
echo ""

# ============================================================
# CHECK 2: 必填字段四层一致性（DDL / ALTER / Entity / VO）
# 对每个 REQUIRED_FIELD，逐实体检查四层是否同步。
# ============================================================
echo "--- CHECK 2: Required field — four-layer consistency ---"
c2_err=0

for entity in "${!ENT_CLASS_TO_FILE[@]}"; do
  efile="${ENT_CLASS_TO_FILE[$entity]}"
  table="${ENT_CLASS_TO_TABLE[$entity]}"

  for field in "${REQ_FIELDS_ARR[@]}"; do
    [[ -z "$field" ]] && continue

    # ── Entity 层 ──
    if ! grep -q "private[[:space:]].*[[:space:]]${field}[[:space:]]*;" "$efile" 2>/dev/null; then
      continue  # 实体无此字段，跳过
    fi

    # ── DDL 初始建表层 ──
    initial_ddl=$(grep -rl "CREATE TABLE.*${table}" --include="*.sql" "$DDL_DIR"/ 2>/dev/null \
      | grep -v '2026/\|alter\|migrate' | head -1 || true)
    if [[ -n "$initial_ddl" ]]; then
      if ! grep -qE "\`${field}\`|${field}[[:space:]]" "$initial_ddl" 2>/dev/null; then
        warn "Initial DDL: $(basename "$initial_ddl") missing column '${field}' for table ${table}"
        ((c2_err++)) || true
      fi
    fi

    # ── ALTER 层：如果 ALTER 添加了该列但初始 DDL 没有，则报错 ──
    alter_files=$(grep -rl "ALTER.*${table}.*ADD.*${field}" --include="*.sql" "$DDL_DIR"/ 2>/dev/null || true)
    if [[ -n "$alter_files" && -n "$initial_ddl" ]]; then
      if ! grep -qE "\`${field}\`|${field}[[:space:]]" "$initial_ddl" 2>/dev/null; then
        err "DDL out-of-sync: ALTER adds '${field}' to ${table} but $(basename "$initial_ddl") missing it"
        ((c2_err++)) || true
      fi
    fi

    # ── VO 层 ──
    vo_found=$(find "$ENTITY_SEARCH_DIR" -name "*VO.java" -type f -exec grep -l "${entity}" {} \; 2>/dev/null || true)
    if [[ -n "$vo_found" ]]; then
      while IFS= read -r vfile; do
        if ! grep -q "private[[:space:]].*[[:space:]]${field}[[:space:]]*;" "$vfile" 2>/dev/null; then
          warn "VO $(basename "$vfile") missing field '${field}' (entity ${entity} has it)"
          ((c2_err++)) || true
        fi
      done <<< "$vo_found"
    fi
  done
done

[[ $c2_err -eq 0 ]] && ok "CHECK 2 passed"
echo ""

# ============================================================
# CHECK 3: 枚举值使用追踪（通用）
# 自动扫描配置中的所有枚举文件，报告未使用的枚举值。
# ============================================================
echo "--- CHECK 3: Enum value usage tracking ---"
c3_warn=0

for enum_file in "${ENUM_FILES_ARR[@]}"; do
  [[ ! -f "$enum_file" ]] && continue
  enum_name=$(basename "$enum_file" .java)

  # 提取所有枚举常量名（匹配 XXX( 模式）
  enum_names=$(grep -oE '^\s+[A-Z_]+\(' "$enum_file" 2>/dev/null \
    | sed 's/[[:space:](]//g' || true)

  unused=0
  while IFS= read -r ename; do
    [[ -z "$ename" ]] && continue
    usage=$(grep -rl "${enum_name}\.${ename}" --include="*.java" "$SERVICE_DIR"/ 2>/dev/null || true)
    if [[ -z "$usage" ]]; then
      warn "${enum_name}.${ename} defined but not used in ${SERVICE_DIR}"
      ((unused++)) || true
    fi
  done <<< "$enum_names"

  if [[ $unused -gt 0 ]]; then
    ((c3_warn += unused)) || true
    warn "CHECK 3: $unused unused enum value(s) in ${enum_name}"
  else
    ok "CHECK 3 passed for ${enum_name}"
  fi
done
echo ""

# ============================================================
# CHECK 4: ALTER ADD COLUMN 与初始 DDL 同步（变更范围）
# 仅检查本次变更的 SQL 文件。
# ============================================================
echo "--- CHECK 4: ALTER vs initial DDL sync ---"
sql_files=$(echo "$changed_files" | grep -E '\.sql$' || true)
c4_err=0

if [[ -n "$sql_files" ]]; then
  while IFS= read -r sf; do
    [[ ! -f "$sf" ]] && continue
    alter_lines=$(grep -iE 'ALTER[[:space:]]+TABLE' "$sf" | grep -i 'ADD[[:space:]]*COLUMN' || true)
    if [[ -n "$alter_lines" ]]; then
      while IFS= read -r line; do
        tbl=$(echo "$line" | sed -n 's/.*ALTER[[:space:]]*TABLE[[:space:]]*`*\([a-zA-Z_][a-zA-Z0-9_]*\)`*.*/\1/Ip' || true)
        col=$(echo "$line" | sed -n 's/.*ADD[[:space:]]*COLUMN[[:space:]]*`*\([a-zA-Z_][a-zA-Z0-9_]*\)`*.*/\1/Ip' || true)
        [[ -z "$tbl" || -z "$col" ]] && continue

        init_ddl=$(grep -rl "CREATE TABLE.*${tbl}" --include="*.sql" "$DDL_DIR"/ 2>/dev/null \
          | grep -v '2026/\|alter\|migrate' | head -1 || true)
        if [[ -n "$init_ddl" ]] && ! grep -q "${col}" "$init_ddl" 2>/dev/null; then
          err "DDL out-of-sync: $(basename "$sf") adds '${col}' to ${tbl}, but $(basename "$init_ddl") missing it"
          ((c4_err++)) || true
        fi
      done <<< "$alter_lines"
    fi
  done <<< "$sql_files"
fi

[[ $c4_err -eq 0 ]] && ok "CHECK 4 passed"
echo ""

# ============================================================
# CHECK 5: DDL 索引幽灵列检测（变更范围）
# 索引引用的列名必须在同一文件中有列定义。
# ============================================================
echo "--- CHECK 5: DDL index ghost columns ---"
c5_err=0

if [[ -n "$sql_files" ]]; then
  while IFS= read -r sf; do
    [[ ! -f "$sf" ]] && continue
    index_cols=$(grep -oE '(KEY|INDEX)[^)]*\([^)]+\)' "$sf" 2>/dev/null \
      | sed 's/.*(\(.*\))/\1/' | sed 's/`//g' || true)
    if [[ -n "$index_cols" ]]; then
      while IFS= read -r col_list; do
        IFS=',' read -ra cols <<< "$col_list"
        for c in "${cols[@]}"; do
          c=$(echo "$c" | sed 's/[` ]//g')
          [[ -z "$c" ]] && continue
          if ! grep -q "\`${c}\`" "$sf" 2>/dev/null; then
            err "Ghost column: $(basename "$sf") index refs '${c}' but no column def found"
            ((c5_err++)) || true
          fi
        done
      done <<< "$index_cols"
    fi
  done <<< "$sql_files"
fi

[[ $c5_err -eq 0 ]] && ok "CHECK 5 passed"
echo ""

# ============================================================
# CHECK 6: DDL 列类型 vs Java 实体类型（通用映射比对）
# 遍历 discovered 实体，对 TYPE_CHECK_FIELDS 中的字段
# 自动比对 DDL 列类型与 Java 字段类型是否匹配。
# ============================================================
echo "--- CHECK 6: DDL vs entity type consistency ---"
c6_err=0

for entity in "${!ENT_CLASS_TO_FILE[@]}"; do
  efile="${ENT_CLASS_TO_FILE[$entity]}"
  table="${ENT_CLASS_TO_TABLE[$entity]}"

  # 查找 DDL 文件（初始 或 ALTER 均可）
  ddl_file=$(grep -rl "CREATE TABLE.*${table}" --include="*.sql" "$DDL_DIR"/ 2>/dev/null | head -1 || true)
  [[ -z "$ddl_file" ]] && continue

  for tfield in "${TYPE_FIELDS_ARR[@]}"; do
    [[ -z "$tfield" ]] && continue

    # ── Java 实体层：提取字段类型 ──
    java_line=$(grep "private[[:space:]].*[[:space:]]${tfield}[[:space:]]*;" "$efile" 2>/dev/null || true)
    [[ -z "$java_line" ]] && continue
    java_type=$(echo "$java_line" | sed 's/.*private[[:space:]]\+\([A-Za-z0-9_]*\)[[:space:]].*/\1/')

    # ── DDL 层：提取列类型 ──
    ddl_line=$(grep -i "\`${tfield}\`" "$ddl_file" 2>/dev/null || true)
    [[ -z "$ddl_line" ]] && continue
    ddl_type=$(echo "$ddl_line" | sed "s/.*\`${tfield}\`[[:space:]]*\([a-zA-Z]*\).*/\1/" | tr '[:upper:]' '[:lower:]')

    # ── 类型映射比对 ──
    matched=false
    for i in "${!JAVA_TYPES[@]}"; do
      if [[ "$java_type" == "${JAVA_TYPES[$i]}" ]]; then
        if echo "$ddl_type" | grep -qiE "^(${SQL_PATTERNS[$i]})"; then
          matched=true
        fi
        break
      fi
    done

    if [[ "$matched" == "false" ]]; then
      err "Type mismatch: $(basename "$ddl_file") ${table}.${tfield} is '${ddl_type}', but $(basename "$efile") uses ${java_type}"
      ((c6_err++)) || true
    fi
  done
done

# 额外: 对所有变更的 DDL+实体对做通用类型扫描（不限于 TYPE_CHECK_FIELDS）
if [[ -n "$sql_files" ]]; then
  while IFS= read -r sf; do
    [[ ! -f "$sf" ]] && continue
    tables_in_ddl=$(grep -oE 'CREATE TABLE[[:space:]]+`?[a-zA-Z_][a-zA-Z0-9_]*`?' "$sf" 2>/dev/null \
      | sed "s/CREATE TABLE[[:space:]]*\`*//;s/\`*$//" || true)
    while IFS= read -r tbl; do
      [[ -z "$tbl" ]] && continue
      # 尝试找到对应实体文件
      for entity in "${!ENT_CLASS_TO_TABLE[@]}"; do
        if [[ "${ENT_CLASS_TO_TABLE[$entity]}" == "$tbl" ]]; then
          efile="${ENT_CLASS_TO_FILE[$entity]}"
          # 逐列比对（仅检查已知的 Java→SQL 映射）
          while IFS= read -r col_line; do
            col_name=$(echo "$col_line" | sed 's/[^`]*`\([a-zA-Z_]*\)`.*/\1/')
            col_type=$(echo "$col_line" | sed 's/[^`]*`[a-zA-Z_]*`[[:space:]]*\([a-zA-Z]*\).*/\1/' | tr '[:upper:]' '[:lower:]')
            # 转 snake_case → camelCase 查 Java 字段
            camel=$(echo "$col_name" | sed 's/_\([a-z]\)/\U\1/g')
            java_line=$(grep "private[[:space:]].*[[:space:]]${camel}[[:space:]]*;" "$efile" 2>/dev/null || true)
            [[ -z "$java_line" ]] && continue
            java_type=$(echo "$java_line" | sed 's/.*private[[:space:]]\+\([A-Za-z0-9_]*\)[[:space:]].*/\1/')
            for i in "${!JAVA_TYPES[@]}"; do
              if [[ "$java_type" == "${JAVA_TYPES[$i]}" ]]; then
                if ! echo "$col_type" | grep -qiE "^(${SQL_PATTERNS[$i]})"; then
                  warn "Broad type check: ${tbl}.${col_name} DDL='${col_type}' vs Java=${java_type} in $(basename "$efile")"
                fi
                break
              fi
            done
          done < <(grep -E '^[[:space:]]+`[a-z_]+`[[:space:]]' "$sf" 2>/dev/null | head -30 || true)
          break
        fi
      done
    done <<< "$tables_in_ddl"
  done <<< "$sql_files"
fi

[[ $c6_err -eq 0 ]] && ok "CHECK 6 passed"
echo ""

# ============================================================
# Summary
# ============================================================
echo "======================================"
echo " Quality Guard Summary"
echo "======================================"
echo -e "  Entities discovered: ${CYAN}${#ENT_CLASS_TO_FILE[@]}${NC}"
echo -e "  Errors:   ${RED}${errors}${NC}"
echo -e "  Warnings: ${YELLOW}${warnings}${NC}"
echo ""

if [[ $errors -gt 0 ]]; then
  echo -e "${RED}FAILED: $errors error(s) found. Fix before committing.${NC}"
  exit 1
fi

if [[ $warnings -gt 0 ]]; then
  echo -e "${YELLOW}PASSED with $warnings warning(s). Consider fixing.${NC}"
  exit 0
fi

echo -e "${GREEN}ALL CHECKS PASSED${NC}"
exit 0
