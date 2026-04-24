/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class WarningSuppressionLogFilter extends Filter<ILoggingEvent> {

    final String[] warningsToSuppress = {
        // https://github.com/javamelody/javamelody/issues/1222
        "Bean 'net.bull.javamelody.JavaMelodyAutoConfiguration' of type [net.bull.javamelody.JavaMelodyAutoConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors",
        "Bean 'monitoringSpringAdvisor' of type [net.bull.javamelody.MonitoringSpringAdvisor] is not eligible for getting processed by all BeanPostProcessors",
        "Bean 'monitoringSpringServiceAdvisor' of type [net.bull.javamelody.MonitoringSpringAdvisor] is not eligible for getting processed by all BeanPostProcessors",
        "Bean 'monitoringSpringControllerAdvisor' of type [net.bull.javamelody.MonitoringSpringAdvisor] is not eligible for getting processed by all BeanPostProcessors",
        "Bean 'monitoringSpringRestControllerAdvisor' of type [net.bull.javamelody.MonitoringSpringAdvisor] is not eligible for getting processed by all BeanPostProcessors",
        "Bean 'monitoringSpringAsyncAdvisor' of type [net.bull.javamelody.MonitoringSpringAdvisor] is not eligible for getting processed by all BeanPostProcessors",
        "Bean 'monitoringSpringScheduledAdvisor' of type [net.bull.javamelody.MonitoringSpringAdvisor] is not eligible for getting processed by all BeanPostProcessors",
        // NOTE: These are logged because we use `runAlways=true` in the liquibase configuration
        "schema \"extensions\" already exists, skipping",
        "extension \"btree_gist\" already exists, skipping",
    };

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel() == Level.WARN) {
            var eventMessage = event.getMessage();
            for (var msg : warningsToSuppress) {
                if (eventMessage.contains(msg)) {
                    return FilterReply.DENY;
                }
            }
        }
        return FilterReply.ACCEPT;
    }
}
