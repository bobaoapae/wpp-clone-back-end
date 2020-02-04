package br.com.zapia.wppclone.utils;

import br.com.zapia.wppclone.modelo.dto.DTO;
import br.com.zapia.wppclone.modelo.dto.DTORelation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.UUID;

public class DTOModelMapper extends RequestResponseBodyMethodProcessor {

    private final ModelMapper modelMapper;
    private EntityManager entityManager;

    public DTOModelMapper(ObjectMapper objectMapper, EntityManager entityManager) {
        super(Collections.singletonList(new MappingJackson2HttpMessageConverter(objectMapper)));
        this.entityManager = entityManager;
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(DTO.class);
    }

    @Override
    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        binder.validate();
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object dto = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        Object id = getEntityId(dto);
        Object obj;
        if (id == null) {
            obj = modelMapper.map(dto, parameter.getParameterType());
        } else {
            obj = entityManager.find(parameter.getParameterType(), id);
            modelMapper.map(dto, obj);
        }
        return resolveRelations(dto, obj);
    }

    @Override
    protected Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        for (Annotation ann : parameter.getParameterAnnotations()) {
            DTO dtoType = AnnotationUtils.getAnnotation(ann, DTO.class);
            if (dtoType != null) {
                return super.readWithMessageConverters(inputMessage, parameter, dtoType.value());
            }
        }
        throw new RuntimeException();
    }

    private Object resolveRelations(Object dto, Object entity) {
        for (Field field : dto.getClass().getDeclaredFields()) {
            DTORelation annotation = field.getAnnotation(DTORelation.class);
            if (annotation != null) {
                try {
                    field.setAccessible(true);
                    Object objRelation = field.get(dto);
                    Object entityId = getEntityId(objRelation);
                    if (entityId != null) {
                        String fieldName;
                        if (annotation.fieldName().isEmpty()) {
                            fieldName = field.getName();
                        } else {
                            fieldName = annotation.fieldName();
                        }
                        Field declaredField = entity.getClass().getDeclaredField(fieldName);
                        declaredField.setAccessible(true);
                        declaredField.set(entity, entityManager.find(declaredField.getType(), entityId));
                    }
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return entity;
    }

    private Object getEntityId(@NotNull Object dto) {
        if (dto instanceof UUID) {
            return dto;
        }
        for (Field field : dto.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Id.class) != null) {
                try {
                    field.setAccessible(true);
                    return field.get(dto);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }
}
