package br.com.zapia.wppclone.utils;

import br.com.zapia.wppclone.modelo.dto.DTO;
import br.com.zapia.wppclone.modelo.dto.DTORelation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.modelmapper.ModelMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.UUID;

public class DTOModelMapper extends RequestResponseBodyMethodProcessor {

    private final ModelMapper modelMapper;
    private final EntityManager entityManager;

    public DTOModelMapper(ObjectMapper objectMapper, EntityManager entityManager, ModelMapper modelMapper) {
        super(Collections.singletonList(new MappingJackson2HttpMessageConverter(objectMapper)));
        this.entityManager = entityManager;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(DTO.class);
    }

    @Override
    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        binder.validate();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object dto = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        Object id = getEntityId(dto);
        Object obj;
        if (id == null) {
            obj = modelMapper.map(dto, parameter.getParameterType());
        } else {
            obj = entityManager.find(parameter.getParameterType(), id);
            if (obj == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, parameter.getParameterType().getSimpleName() + " with id:" + id + " was not found.");
            }
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Object resolveRelations(Object dto, Object entity) {
        for (Field field : dto.getClass().getDeclaredFields()) {
            DTORelation annotation = field.getAnnotation(DTORelation.class);
            if (annotation != null) {
                try {
                    field.setAccessible(true);
                    Object objRelation = field.get(dto);
                    String fieldName;
                    if (annotation.fieldName().isEmpty()) {
                        fieldName = field.getName();
                    } else {
                        fieldName = annotation.fieldName();
                    }
                    Field declaredField = entity.getClass().getDeclaredField(fieldName);
                    declaredField.setAccessible(true);
                    if (objRelation != null) {
                        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
                        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(annotation.classEntidade());
                        Root root = criteriaQuery.from(annotation.classEntidade());
                        criteriaQuery.select(root).where(criteriaBuilder.equal(root.get(annotation.key()), objRelation));
                        Object obj = DataAccessUtils.singleResult(entityManager.createQuery(criteriaQuery).getResultList());
                        if (obj == null) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DTO Relation To:" + annotation.classEntidade().getSimpleName() + " with key('" + annotation.key() + "'):" + objRelation + " was not found.");
                        }
                        declaredField.set(entity, obj);
                    } else {
                        declaredField.set(entity, null);
                    }
                } catch (ResponseStatusException e) {
                    throw e;
                } catch (Exception e) {
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
