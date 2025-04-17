### WatchTogether 项目结构
```angular2html
WatchTogetherBackend
├── wt-common   // 通用工具类、封装结果、常量等
├── wt-entity   // 实体类、DTO、VO、枚举等
├── wt-dao      // Mapper 接口与 XML
├── wt-service  // 业务接口与实现
└── wt-web      // Controller、WebSocket、配置类、拦截器、过滤器等

// 继承关系
wt-web -> wt-service -> wt-dao -> wt-common -> entity
```

