package com.noahgeerts.progress.config;

import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

@Configuration
public class ProgressConfig {
  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer strictJacksonCustomizer() {
    return builder -> builder.postConfigurer(mapper -> {
      mapper.coercionConfigFor(LogicalType.Integer)
          .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
      mapper.coercionConfigFor(LogicalType.Float)
          .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
      mapper.coercionConfigFor(LogicalType.Textual)
          .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
      mapper.coercionConfigFor(LogicalType.Textual)
          .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    });
  }
}
