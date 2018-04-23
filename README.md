### FastMultidex

#### 使用

仅debug构建启用，使用前请禁用multidex，具体配置如下

```
android{
    defaultConfig {
        multiDexEnabled false
    }
    
    buildTypes {
        debug {
            multiDexEnabled false
        }
        release {
            multiDexEnabled true
        }
    }
}
```

如需删除构建缓存，执行**gradlew cleanFastMultidexCache**即可

#### 原理

核心原理是自己计算mainDexList，拆jar，粒度越细，效果越佳，缓存的粒度也会越细。

 - 禁用系统multidex后，会禁用jarMerging和计算multidexList的任务
 - 禁用以上任务后，需要自己手动计算mainDexList
 - 禁用以上任务后，需要自己手动对目录下的class进行jarMerge
 - 根据计算出来的mainDexList和输入文件，进行重新打包，将mainDexList中的类打包到mainDex.jar，并从其他jar中删除
 - 将前一步产生的jar文件进行jar2dex转换，并进行相应算法将文件缓存起来，以便后续复用缓存
 - 将产生的classes.dex进行重命名，mainDex.jar产生的dex命名为classes.dex，其他dex依次从2开始递增，如classes2.dex


#### 存在的问题

 - 首次在无缓存的情况下构建会比较耗时
 - 在已有缓存基础上，构建会大大提速
 - 不支持输入文件为单一文件的提速