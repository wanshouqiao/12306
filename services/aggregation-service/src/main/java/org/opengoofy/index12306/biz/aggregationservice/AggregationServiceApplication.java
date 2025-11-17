package org.opengoofy.index12306.biz.aggregationservice;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import cn.hippo4j.core.enable.EnableDynamicThreadPool;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 12306 聚合服务应用启动器
 */
@EnableDynamicThreadPool  // 启用 Hippo4j 提供的动态线程池能力，便于运行时调整线程池大小、阈值等
@SpringBootApplication(scanBasePackages = {
        // 启动 Spring Boot，并指定要扫描的基础包。聚合服务需要整合用户、车票、订单、支付、以及自身模块，
        // 所以列出这些包以便加载到同一个应用上下文
        "org.opengoofy.index12306.biz.userservice",
        "org.opengoofy.index12306.biz.ticketservice",
        "org.opengoofy.index12306.biz.orderservice",
        "org.opengoofy.index12306.biz.payservice",
        "org.opengoofy.index12306.biz.aggregationservice"
})
@EnableRetry // 开启 Spring Retry，允许在需要的地方通过 @Retryable 实现重试机制，应对外部调用失败等情况
@MapperScan(value = {
        // 自动扫描 MyBatis Mapper 接口，避免每个接口上都加 @Mapper，并确保各业务模块的 DAO 层能被识别
        "org.opengoofy.index12306.biz.userservice.dao.mapper",
        "org.opengoofy.index12306.biz.ticketservice.dao.mapper",
        "org.opengoofy.index12306.biz.orderservice.dao.mapper",
        "org.opengoofy.index12306.biz.payservice.dao.mapper"
})
@EnableFeignClients(value = {
        // 开启 Feign 客户端，指定远程调用接口所在包，聚合服务通过这些客户端去调用其他微服务。
        "org.opengoofy.index12306.biz.ticketservice.remote",
        "org.opengoofy.index12306.biz.orderservice.remote"
})
// 启用 Crane4j（数据填充/装配框架），扫描订单服务的枚举包，在聚合层做枚举转换或数据装配时可自动处理。
@EnableCrane4j(enumPackages = "org.opengoofy.index12306.biz.orderservice.common.enums")
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }
}
