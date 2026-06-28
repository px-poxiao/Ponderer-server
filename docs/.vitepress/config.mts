import { defineConfig } from "vitepress";

const repoName = process.env.GITHUB_REPOSITORY?.split("/")[1];

export default defineConfig({
  title: "Ponderer-server",
  description: "Ponderer-server 服主文档",
  base: process.env.GITHUB_ACTIONS && repoName ? `/${repoName}/` : "/",
  lang: "zh-CN",
  cleanUrls: true,
  themeConfig: {
    siteTitle: "Ponderer-server",
    nav: [
      { text: "首页", link: "/" },
      { text: "命令", link: "/commands" },
      { text: "权限", link: "/permissions" },
      { text: "配置", link: "/config" }
    ],
    sidebar: [
      { text: "首页", link: "/" },
      { text: "命令", link: "/commands" },
      { text: "权限", link: "/permissions" },
      { text: "配置", link: "/config" }
    ],
    search: {
      provider: "local"
    },
    outline: {
      label: "本页目录",
      level: [2, 3]
    },
    docFooter: {
      prev: "上一页",
      next: "下一页"
    },
    lastUpdated: {
      text: "最后更新",
      formatOptions: {
        dateStyle: "medium",
        timeStyle: "short"
      }
    }
  },
  lastUpdated: true
});
