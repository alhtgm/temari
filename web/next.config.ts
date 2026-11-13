import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // スタッフ画面は開くたびに最新のデータを出したいので、ページを前もって作り置きする機能（cacheComponents など）は使わない。
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
