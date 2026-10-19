-- ----------------------------
-- 电子书表
-- ----------------------------
DROP TABLE IF EXISTS `osh_book`;
CREATE TABLE `osh_book` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '电子书ID',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `cover` varchar(500) DEFAULT NULL COMMENT '封面',
  `description` text COMMENT '描述',
  `try_content` text COMMENT '试读内容',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT '0.00' COMMENT '原价',
  `sub_count` int(11) DEFAULT '0' COMMENT '订阅数',
  `level` tinyint(3) DEFAULT NULL COMMENT '图书等级',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1下架）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='电子书表';

-- ----------------------------
-- 电子书章节表
-- ----------------------------
DROP TABLE IF EXISTS `osh_book_chapter`;
CREATE TABLE `osh_book_chapter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `book_id` bigint(20) NOT NULL COMMENT '电子书ID',
  `title` varchar(200) DEFAULT NULL COMMENT '章节标题',
  `content` longtext COMMENT '章节内容',
  `orderby` int(11) DEFAULT '0' COMMENT '排序',
  `isfree` tinyint(1) DEFAULT '0' COMMENT '是否免费（0收费 1免费）',
  `is_free` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否免费章节（0-否 1-是，Mapper引用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除（拦截器引用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='电子书章节表';

-- ----------------------------
-- 用户电子书表
-- ----------------------------
DROP TABLE IF EXISTS `osh_user_book`;
CREATE TABLE `osh_user_book` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户电子书ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `book_id` bigint(20) NOT NULL COMMENT '电子书ID',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户电子书表';

-- ----------------------------
-- 插入测试数据
-- ----------------------------
-- 电子书数据
INSERT INTO `osh_book` VALUES (1, 'uni-app实战直播app全栈开发', 'http://example.com/cover1.png', 'uni-app全栈开发教程', '<p>这是试读内容，介绍uni-app基础知识</p>', 0.10, 398.00, 2, '0', '0', 'admin', now(), '', NULL, NULL);
INSERT INTO `osh_book` VALUES (2, '第1本电子书', 'http://example.com/cover2.png', '第1本电子书', '<p>图文介绍，讲解前端基础知识</p>', 9.98, 20.00, 5, '0', '0', 'admin', now(), '', NULL, NULL);
INSERT INTO `osh_book` VALUES (32, 'Vue3 + TypeScript 全栈开发', 'http://example.com/cover32.png', 'Vue3全栈开发完整教程', '<p>从零开始学习Vue3和TypeScript</p>', 19.90, 99.00, 10, '0', '0', 'admin', now(), '', NULL, NULL);

-- 电子书章节数据
-- Book ID=1 的章节（uni-app实战）
INSERT INTO `osh_book_chapter` VALUES (1, 1, '什么是 uni-app', '<h2>什么是 uni-app</h2><p><code>uni-app</code> 是一个使用 <a href="https://vuejs.org/">Vue.js</a> 开发所有前端应用的框架，开发者编写一套代码，可发布到iOS、Android、Web（响应式）、以及各种小程序（微信/支付宝/百度/头条/飞书/QQ/快手/钉钉/淘宝）、快应用等多个平台。</p><h3>核心特点</h3><ul><li>一套代码，多端运行</li><li>基于Vue.js语法</li><li>完善的组件和API</li><li>丰富的插件生态</li></ul>', 0, 1, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (2, 1, 'uni-app 环境搭建', '<h2>uni-app 环境搭建</h2><p>开发 uni-app 需要安装 <strong>HBuilderX</strong> 编辑器。</p><h3>安装步骤</h3><ol><li>下载 HBuilderX: <a href="https://www.dcloud.io/hbuilderx.html">官网下载</a></li><li>解压并运行 HBuilderX</li><li>新建 uni-app 项目</li><li>选择模板开始开发</li></ol><pre><code>// 创建项目命令\nvue create -p dcloudio/uni-preset-vue my-project</code></pre>', 1, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (3, 1, 'uni-app 项目结构', '<h2>uni-app 项目结构</h2><p>一个 uni-app 项目，默认包含如下目录及文件：</p><pre><code>├── pages          // 页面文件\n├── static         // 静态资源\n├── components     // 组件\n├── App.vue        // 应用配置\n├── main.js        // 入口文件\n├── manifest.json  // 配置文件\n└── pages.json     // 页面路由</code></pre><h3>重要文件说明</h3><ul><li><code>pages.json</code>: 配置页面路由、导航栏、tabBar等</li><li><code>manifest.json</code>: 配置应用名称、appid、logo等</li><li><code>App.vue</code>: 应用生命周期、全局样式</li></ul>', 2, 1, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (4, 1, '生命周期与数据绑定', '<h2>生命周期与数据绑定</h2><h3>应用生命周期</h3><p>uni-app 支持如下应用生命周期函数：</p><ul><li><code>onLaunch</code>: 应用初始化完成时触发（全局只触发一次）</li><li><code>onShow</code>: 应用启动，或从后台进入前台</li><li><code>onHide</code>: 应用从前台进入后台</li></ul><h3>数据绑定示例</h3><pre><code>&lt;template&gt;\n  &lt;view&gt;\n    &lt;text&gt;{{ message }}&lt;/text&gt;\n  &lt;/view&gt;\n&lt;/template&gt;\n\n&lt;script&gt;\nexport default {\n  data() {\n    return {\n      message: \'Hello uni-app\'\n    }\n  }\n}\n&lt;/script&gt;</code></pre>', 3, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (5, 1, 'uni-app 路由与导航', '<h2>uni-app 路由与导航</h2><h3>页面跳转</h3><p>uni-app 有如下几种页面跳转方式：</p><pre><code>// 保留当前页面，跳转到应用内的某个页面\nuni.navigateTo({\n  url: \'/pages/detail/detail?id=1\'\n});\n\n// 关闭当前页面，跳转到应用内的某个页面\nuni.redirectTo({\n  url: \'/pages/info/info\'\n});\n\n// 跳转到 tabBar 页面\nuni.switchTab({\n  url: \'/pages/index/index\'\n});</code></pre><h3>路由传参</h3><p>在跳转时可以通过 URL 传递参数，在目标页面的 onLoad 生命周期中接收。</p>', 4, 0, '0', 'admin', now(), '', NULL, NULL);

-- Book ID=2 的章节（第1本电子书）
INSERT INTO `osh_book_chapter` VALUES (6, 2, '引入全局图标库', '<h2>引入全局图标库</h2><p>本章介绍如何在项目中引入全局图标库，如 <strong>Font Awesome</strong> 或 <strong>iconfont</strong>。</p><h3>使用 iconfont</h3><ol><li>访问 <a href="https://www.iconfont.cn/">iconfont 官网</a></li><li>选择需要的图标加入购物车</li><li>下载到本地项目</li><li>在项目中引入 CSS 文件</li></ol><pre><code>// 在 main.js 中引入\nimport \'./static/iconfont/iconfont.css\';</code></pre>', 0, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (7, 2, 'CSS 预处理器 Sass', '<h2>CSS 预处理器 Sass</h2><p>Sass 是一个成熟、稳定、强大的 CSS 预处理器。</p><h3>安装 Sass</h3><pre><code>npm install sass sass-loader --save-dev</code></pre><h3>在 Vue 组件中使用</h3><pre><code>&lt;style lang="scss" scoped&gt;\n$primary-color: #409EFF;\n\n.container {\n  .title {\n    color: $primary-color;\n    font-size: 20px;\n  }\n}\n&lt;/style&gt;</code></pre>', 1, 1, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (8, 2, 'Vue 组件通信', '<h2>Vue 组件通信</h2><h3>父子组件通信</h3><p>父组件通过 <code>props</code> 向子组件传递数据，子组件通过 <code>$emit</code> 向父组件发送事件。</p><pre><code>// 父组件\n&lt;Child :message="parentMsg" @childEvent="handleEvent" /&gt;\n\n// 子组件\nexport default {\n  props: [\'message\'],\n  methods: {\n    sendEvent() {\n      this.$emit(\'childEvent\', data);\n    }\n  }\n}</code></pre>', 2, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (9, 2, 'Vuex 状态管理', '<h2>Vuex 状态管理</h2><p>Vuex 是一个专为 Vue.js 应用程序开发的<strong>状态管理模式</strong>。</p><h3>核心概念</h3><ul><li><strong>State</strong>: 存储状态数据</li><li><strong>Getter</strong>: 从 state 中派生出的状态</li><li><strong>Mutation</strong>: 更改 state 的唯一方法</li><li><strong>Action</strong>: 提交 mutation，可包含异步操作</li><li><strong>Module</strong>: 将 store 分割成模块</li></ul><pre><code>// store.js\nexport default new Vuex.Store({\n  state: {\n    count: 0\n  },\n  mutations: {\n    increment(state) {\n      state.count++\n    }\n  }\n})</code></pre>', 3, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (10, 2, 'Vue Router 路由配置', '<h2>Vue Router 路由配置</h2><p>Vue Router 是 Vue.js 官方的路由管理器。</p><h3>基本使用</h3><pre><code>import VueRouter from \'vue-router\';\n\nconst routes = [\n  { path: \'/\', component: Home },\n  { path: \'/about\', component: About }\n];\n\nconst router = new VueRouter({\n  routes\n});\n\nexport default router;</code></pre><h3>动态路由</h3><pre><code>// 路由配置\n{ path: \'/oshUser/:id\', component: User }\n\n// 获取参数\nthis.$route.params.id</code></pre>', 4, 0, '0', 'admin', now(), '', NULL, NULL);

-- Book ID=32 的章节（Vue3 + TypeScript）
INSERT INTO `osh_book_chapter` VALUES (61, 32, '什么是 Vue 3', '<h2>什么是 Vue 3</h2><p><strong>Vue 3</strong> 是 Vue.js 的最新主要版本，带来了许多激动人心的新特性和改进。</p><h3>Vue 3 的主要特性</h3><ul><li><strong>Composition API</strong>: 更灵活的组织组件逻辑</li><li><strong>更好的 TypeScript 支持</strong>: 完全用 TypeScript 重写</li><li><strong>性能提升</strong>: 更快的虚拟 DOM，更小的打包体积</li><li><strong>Fragment</strong>: 支持多个根节点</li><li><strong>Teleport</strong>: 传送门组件</li><li><strong>Suspense</strong>: 异步组件的优雅处理</li></ul><h3>与 Vue 2 的对比</h3><table><tr><th>特性</th><th>Vue 2</th><th>Vue 3</th></tr><tr><td>组合逻辑</td><td>Options API</td><td>Composition API</td></tr><tr><td>性能</td><td>较快</td><td>更快（提升约 1.3-2 倍）</td></tr><tr><td>TypeScript</td><td>部分支持</td><td>完全支持</td></tr><tr><td>打包体积</td><td>~20KB</td><td>~10KB (tree-shaking)</td></tr></table>', 0, 1, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (62, 32, 'Vue 3 项目搭建', '<h2>Vue 3 项目搭建</h2><h3>使用 Vite 创建项目</h3><p>Vite 是下一代前端构建工具，提供极速的开发体验。</p><pre><code># 使用 npm\nnpm create vite@latest my-vue-app -- --template vue-ts\n\n# 使用 yarn\nyarn create vite my-vue-app --template vue-ts\n\n# 使用 pnpm\npnpm create vite my-vue-app --template vue-ts</code></pre><h3>安装依赖</h3><pre><code>cd my-vue-app\nnpm install\nnpm run dev</code></pre><h3>项目结构</h3><pre><code>my-vue-app/\n├── public/\n├── src/\n│   ├── assets/\n│   ├── components/\n│   ├── App.vue\n│   └── main.ts\n├── index.html\n├── package.json\n├── tsconfig.json\n└── vite.config.ts</code></pre>', 1, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (63, 32, 'Composition API 详解', '<h2>Composition API 详解</h2><p>Composition API 是 Vue 3 最重要的新特性之一。</p><h3>基本使用</h3><pre><code>import { ref, reactive, computed } from \'vue\';\n\nexport default {\n  setup() {\n    // 响应式数据\n    const count = ref(0);\n    const oshUser = reactive({\n      name: \'张三\',\n      age: 25\n    });\n\n    // 计算属性\n    const doubleCount = computed(() => count.value * 2);\n\n    // 方法\n    const increment = () => {\n      count.value++;\n    };\n\n    return {\n      count,\n      oshUser,\n      doubleCount,\n      increment\n    };\n  }\n};</code></pre><h3>生命周期钩子</h3><pre><code>import { onMounted, onUnmounted } from \'vue\';\n\nsetup() {\n  onMounted(() => {\n    console.log(\'组件已挂载\');\n  });\n\n  onUnmounted(() => {\n    console.log(\'组件已卸载\');\n  });\n}</code></pre>', 2, 0, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (64, 32, 'TypeScript 基础', '<h2>TypeScript 基础</h2><p>TypeScript 是 JavaScript 的超集，添加了类型系统和其他特性。</p><h3>基本类型</h3><pre><code>// 基础类型\nlet isDone: boolean = false;\nlet count: number = 10;\nlet name: string = "张三";\n\n// 数组\nlet list: number[] = [1, 2, 3];\nlet list2: Array&lt;number&gt; = [1, 2, 3];\n\n// 元组\nlet tuple: [string, number] = [\'hello\', 10];\n\n// 枚举\nenum Color { Red, Green, Blue }\nlet c: Color = Color.Green;</code></pre><h3>接口</h3><pre><code>interface User {\n  id: number;\n  name: string;\n  age?: number;  // 可选属性\n  readonly email: string;  // 只读属性\n}\n\nconst oshUser: User = {\n  id: 1,\n  name: \'张三\',\n  email: \'zhang@example.com\'\n};</code></pre>', 3, 1, '0', 'admin', now(), '', NULL, NULL);

INSERT INTO `osh_book_chapter` VALUES (65, 32, 'Vue 3 + TS 组件开发', '<h2>Vue 3 + TypeScript 组件开发</h2><h3>使用 defineComponent</h3><pre><code>import { defineComponent, ref, PropType } from \'vue\';\n\ninterface User {\n  id: number;\n  name: string;\n}\n\nexport default defineComponent({\n  name: \'UserCard\',\n  props: {\n    oshUser: {\n      type: Object as PropType&lt;User&gt;,\n      required: true\n    }\n  },\n  setup(props) {\n    const greeting = ref&lt;string&gt;(\'Hello\');\n    \n    return {\n      greeting\n    };\n  }\n});</code></pre><h3>使用 &lt;script setup&gt;</h3><pre><code>&lt;script setup lang="ts"&gt;\nimport { ref } from \'vue\';\n\ninterface User {\n  id: number;\n  name: string;\n}\n\nconst props = defineProps&lt;{\n  oshUser: User;\n}&gt;();\n\nconst count = ref(0);\nconst increment = () => count.value++;\n&lt;/script&gt;</code></pre>', 4, 0, '0', 'admin', now(), '', NULL, NULL);

-- ----------------------------
-- 用户购买记录（测试数据）
-- ----------------------------
-- admin 用户（user_id=1）购买的电子书
INSERT INTO `osh_user_book` VALUES (1, 1, 2, '0', 'admin', now());
INSERT INTO `osh_user_book` VALUES (2, 1, 32, '0', 'admin', now());

-- ry 测试用户（user_id=2）购买的电子书
INSERT INTO `osh_user_book` VALUES (3, 2, 1, '0', 'admin', now());

-- ----------------------------
-- 用户电子书关联表（收藏、关注、购买）
-- ----------------------------
DROP TABLE IF EXISTS `osh_user_book_relation`;
CREATE TABLE `osh_user_book_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `book_id` bigint(20) NOT NULL COMMENT '电子书ID',
  `favorited` tinyint(1) DEFAULT '0' COMMENT '是否收藏（0-否，1-是）',
  `followed` tinyint(1) DEFAULT '0' COMMENT '是否关注（0-否，1-是）',
  `purchased` tinyint(1) DEFAULT '0' COMMENT '是否购买（0-否，1-是）',
  `favorite_time` datetime DEFAULT NULL COMMENT '收藏时间',
  `follow_time` datetime DEFAULT NULL COMMENT '关注时间',
  `purchase_price` decimal(10,2) DEFAULT NULL COMMENT '购买价格',
  `order_no` varchar(64) DEFAULT NULL COMMENT '订单号',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除（0-否，1-是）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户电子书关联表';
