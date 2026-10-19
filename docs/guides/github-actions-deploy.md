# 后端 GitHub Actions 自动部署教程

这份教程给第一次使用 GitHub Actions 的人看。你只要按步骤配置一次，以后后端代码推送到 `release/20260328` 分支后，GitHub 会自动构建 jar、上传服务器并重启后端服务。

## 1. 这套自动部署会做什么

以前手动部署后端一般要做这些事：本地 Maven 打包、上传 jar、进入服务器、重启 Docker Compose 服务。现在这些事交给 GitHub Actions 自动做。

自动部署流程如下：

1. GitHub 拉取 `release/20260328` 分支的最新代码。
2. 安装 JDK 17。
3. 执行 `mvn clean install -DskipTests -pl '!backstage-flink'` 构建后端。
4. 检查 `backstage-admin/target/backstage-admin.jar` 是否存在。
5. 检查 jar 包能不能正常解压。
6. 用 SSH 登录服务器。
7. 停止 `backstage-admin` 容器，避免 Java 进程读取上传到一半的 jar。
8. 使用 `rsync --inplace` 上传 jar 到 `/www/ruoyi/backstage-admin.jar`。
9. 对比本地和服务器 SHA256，确认 jar 完整一致。
10. 在服务器执行 `unzip -t`，确认 jar 里面的依赖包可读。
11. 启动 `backstage-admin` 容器。
12. 输出容器状态和最近日志，方便排查。

对应 workflow 文件在：

```text
.github/workflows/deploy-release.yml
```

## 2. 第一次使用前必须准备什么

你需要准备 3 样东西：

| 要准备的东西 | 说明 |
| --- | --- |
| GitHub 仓库权限 | 你需要能进入仓库的 `Settings` 页面添加 Secret。 |
| 服务器 SSH 私钥 | GitHub Actions 用它登录服务器。 |
| release 分支 | 当前自动部署监听的是 `release/20260328`。 |

服务器公钥我已经配置过，服务器允许这把部署 key 登录。你只需要把私钥放进 GitHub Secret。

## 3. 配置 GitHub Secret

Secret 是 GitHub 保存敏感信息的地方。SSH 私钥不能写在代码里，必须放到 Secret。

### 3.1 复制私钥

在本机终端执行：

```bash
pbcopy < /Users/rengang/.ssh/osh_github_actions_deploy_key
```

执行后不会输出内容，这是正常的。私钥已经复制到剪贴板。

注意：不要把私钥发到聊天、文档、Issue 或代码里。

### 3.2 在 GitHub 页面添加 Secret

打开后端仓库：

```text
https://github.com/juege-osh/osh-backend
```

按下面路径点击：

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

填写：

| 输入框 | 填什么 |
| --- | --- |
| Name | `DEPLOY_SSH_PRIVATE_KEY` |
| Secret | 粘贴刚才复制的私钥 |

最后点击 `Add secret` 保存。

## 4. 怎么触发自动部署

有两种方式。

### 4.1 推送代码自动部署

只要你把代码推送到：

```text
release/20260328
```

GitHub Actions 就会自动运行。

### 4.2 手动点击部署

如果你只是想重新部署一次，不想改代码，可以手动点。

操作路径：

```text
后端仓库 -> Actions -> Deploy Backend Release -> Run workflow
```

选择分支：

```text
release/20260328
```

然后点击绿色按钮 `Run workflow`。

## 5. 怎么判断部署成功

进入后端仓库的 `Actions` 页面，点开最新的一条 `Deploy Backend Release`。

如果看到绿色对勾，说明成功。

成功时一般会看到这些步骤都通过：

| 步骤 | 成功说明 |
| --- | --- |
| `Build backend jar` | Maven 构建成功。 |
| `Validate jar output` | jar 存在且可解压。 |
| `Configure SSH` | SSH key 写入成功。 |
| `Stop backend before replacing jar` | 能连上服务器并停止后端。 |
| `Upload backend jar safely` | jar 上传成功。 |
| `Verify remote jar integrity` | 本地和服务器 jar 的 SHA256 一致。 |
| `Restart backend service` | 后端容器启动成功。 |
| `Show backend logs` | 能看到后端最近日志。 |

部署成功后访问：

```text
http://43.242.200.25:8081/
```

如果返回 HTTP 200，说明后端已经正常运行。

## 6. 常用服务器信息

workflow 已经内置下面这些默认值，正常不用改：

| 配置 | 默认值 |
| --- | --- |
| 服务器 IP | `43.242.200.25` |
| SSH 用户 | `root` |
| SSH 端口 | `58753` |
| jar 上传目录 | `/www/ruoyi` |
| jar 文件名 | `backstage-admin.jar` |
| Docker Compose 目录 | `/opt/docker-apps` |
| 后端服务名 | `backstage-admin` |
| JDK 版本 | `17` |

如果以后服务器换了，不要改代码，优先在 GitHub Variables 里覆盖：

| Variable | 用途 |
| --- | --- |
| `DEPLOY_HOST` | 覆盖服务器 IP 或域名。 |
| `DEPLOY_USER` | 覆盖 SSH 用户。 |
| `DEPLOY_PORT` | 覆盖 SSH 端口。 |
| `BACKEND_DEPLOY_DIR` | 覆盖 jar 上传目录。 |
| `BACKEND_JAR_NAME` | 覆盖 jar 文件名。 |
| `DOCKER_COMPOSE_DIR` | 覆盖 Docker Compose 目录。 |
| `BACKEND_SERVICE_NAME` | 覆盖后端服务名。 |

配置位置：

```text
Settings -> Secrets and variables -> Actions -> Variables
```

## 7. 本地手动测试命令

如果 GitHub Actions 失败，可以先在本地跑一遍。

进入后端项目：

```bash
cd /Users/rengang/chuangye/osh-projects/osh-backend
```

本地构建：

```bash
JAVA_HOME=/Users/rengang/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  clean install -DskipTests -pl '!backstage-flink'
```

检查 jar 是否存在：

```bash
test -f backstage-admin/target/backstage-admin.jar
```

检查 jar 是否能解压：

```bash
unzip -t backstage-admin/target/backstage-admin.jar >/tmp/backend-jar-check.log
```

如果这些步骤本地都成功，说明后端代码本身可以打包。

## 8. 本地手动上传后端

正常不需要手动上传，只有排查问题时才用。

先停止后端，避免服务读取上传到一半的 jar：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "mkdir -p /www/ruoyi && cd /opt/docker-apps && docker-compose stop backstage-admin"
```

上传 jar：

```bash
rsync -az --inplace --progress \
  -e "ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753" \
  backstage-admin/target/backstage-admin.jar \
  root@43.242.200.25:/www/ruoyi/backstage-admin.jar
```

对比本地和服务器 SHA256：

```bash
LOCAL_SHA=$(shasum -a 256 backstage-admin/target/backstage-admin.jar | awk '{print $1}')
REMOTE_SHA=$(ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "sha256sum /www/ruoyi/backstage-admin.jar | cut -d ' ' -f 1")
echo "local=$LOCAL_SHA"
echo "remote=$REMOTE_SHA"
test "$LOCAL_SHA" = "$REMOTE_SHA"
```

如果 `test` 没有报错，说明两个文件完全一样。

检查服务器 jar 是否可读：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "unzip -t /www/ruoyi/backstage-admin.jar 'BOOT-INF/lib/*' >/tmp/backend-jar-check.log && tail -5 /tmp/backend-jar-check.log"
```

启动后端：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "cd /opt/docker-apps && docker-compose up -d backstage-admin && sleep 20 && docker-compose ps backstage-admin"
```

## 9. 部署后验证命令

检查后端容器是否运行：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "cd /opt/docker-apps && docker-compose ps backstage-admin"
```

成功时 `STATUS` 应该是 `Up`。

检查后端 HTTP 是否正常：

```bash
curl -I http://43.242.200.25:8081/
```

成功时能看到类似：

```text
HTTP/1.1 200
```

查看后端日志：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "docker logs -f backstage-admin"
```

看到“若依启动成功”或持续正常日志，说明后端已经启动。

## 10. 常见问题

### 10.1 卡在 Configure SSH 或 Stop backend before replacing jar

这通常是 GitHub 不能 SSH 登录服务器。

优先检查：

| 检查项 | 怎么处理 |
| --- | --- |
| `DEPLOY_SSH_PRIVATE_KEY` 是否存在 | 去 GitHub 仓库 `Settings -> Secrets and variables -> Actions` 看。 |
| Secret 名字是否写错 | 必须完全等于 `DEPLOY_SSH_PRIVATE_KEY`。 |
| Secret 内容是否完整 | 重新执行 `pbcopy < /Users/rengang/.ssh/osh_github_actions_deploy_key`，再覆盖保存。 |
| 服务器端口是否正确 | 当前是 `58753`。 |

### 10.2 Build backend jar 很慢

后端 Maven 构建依赖多，第一次在 GitHub Runner 上构建会比较慢。只要不是红色失败，可以先等。

如果最终失败，再点开 `Build backend jar` 看日志。

### 10.3 构建失败

先在本地执行：

```bash
cd /Users/rengang/chuangye/osh-projects/osh-backend
mvn clean install -DskipTests -pl '!backstage-flink'
```

如果本地也失败，说明是代码编译问题，需要先修代码。

### 10.4 容器没有启动成功

检查服务名和容器状态：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "cd /opt/docker-apps && docker-compose ps"
```

如果服务名不是 `backstage-admin`，需要修改 GitHub Variable：

```text
BACKEND_SERVICE_NAME
```

### 10.5 jar 显示 Invalid or corrupt jarfile

这通常是上传中断后把半包覆盖到了 `/www/ruoyi/backstage-admin.jar`。

处理方式：

1. 先停止后端。
2. 用 `rsync --inplace` 重新上传完整 jar。
3. 对比 SHA256。
4. SHA 一致后再启动后端。

命令如下：

```bash
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "cd /opt/docker-apps && docker-compose stop backstage-admin"

cd /Users/rengang/chuangye/osh-projects/osh-backend
rsync -az --inplace --progress \
  -e "ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753" \
  backstage-admin/target/backstage-admin.jar \
  root@43.242.200.25:/www/ruoyi/backstage-admin.jar

shasum -a 256 backstage-admin/target/backstage-admin.jar
ssh -i /Users/rengang/.ssh/osh_github_actions_deploy_key -p 58753 root@43.242.200.25 \
  "sha256sum /www/ruoyi/backstage-admin.jar && cd /opt/docker-apps && docker-compose up -d backstage-admin"
```

### 10.6 Actions 是黄色圆圈

黄色圆圈表示还在运行，不是失败。后端第一次 Maven 构建可能比较久。

### 10.7 Actions 是红色叉

红色叉表示失败。点进去看哪个步骤红了，再按上面的常见问题排查。
