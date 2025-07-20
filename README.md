# Decentralized Instant Messaging (Java SDK)


[![License](https://img.shields.io/github/license/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/blob/master/LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dimchat/sdk-java/pulls)
[![Platform](https://img.shields.io/badge/Platform-Java%208-brightgreen.svg)](https://github.com/dimchat/sdk-java/wiki)
[![Issues](https://img.shields.io/github/issues/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/issues)
[![Repo Size](https://img.shields.io/github/repo-size/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/archive/refs/heads/main.zip)
[![Tags](https://img.shields.io/github/tag/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/tags)
[![Version](https://img.shields.io/maven-central/v/chat.dim/SDK)](https://mvnrepository.com/artifact/chat.dim/SDK)

[![Watchers](https://img.shields.io/github/watchers/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/watchers)
[![Forks](https://img.shields.io/github/forks/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/forks)
[![Stars](https://img.shields.io/github/stars/dimchat/sdk-java)](https://github.com/dimchat/sdk-java/stargazers)
[![Followers](https://img.shields.io/github/followers/dimchat)](https://github.com/orgs/dimchat/followers)

## Dependencies

* Latest Versions

| Name | Version | Description |
|------|---------|-------------|
| [Ming Ke Ming (名可名)](https://github.com/dimchat/mkm-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/MingKeMing)](https://mvnrepository.com/artifact/chat.dim/MingKeMing) | Decentralized User Identity Authentication |
| [Dao Ke Dao (道可道)](https://github.com/dimchat/dkd-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DaoKeDao)](https://mvnrepository.com/artifact/chat.dim/DaoKeDao) | Universal Message Module |
| [DIMP (去中心化通讯协议)](https://github.com/dimchat/core-java) | [![Version](https://img.shields.io/maven-central/v/chat.dim/DIMP)](https://mvnrepository.com/artifact/chat.dim/DIMP) | Decentralized Instant Messaging Protocol |

* build.gradle

```javascript
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

dependencies {
    // https://bintray.com/dimchat/common/sdk
    implementation group: 'chat.dim', name: 'SDK', version: '2.0.0'
}
```

* pom.xml

```xml
<dependencies>

    <!-- https://mvnrepository.com/artifact/chat.dim/SDK -->
    <dependency>
        <groupId>chat.dim</groupId>
        <artifactId>SDK</artifactId>
        <version>2.0.0</version>
        <type>pom</type>
    </dependency>

</dependencies>
```

Copyright &copy; 2018-2025 Albert Moky
[![Followers](https://img.shields.io/github/followers/moky)](https://github.com/moky?tab=followers)