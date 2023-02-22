package grails.plugin.inertia.annotation;

import grails.artefact.Interceptor;
import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.core.GrailsControllerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

public class AnnotationExcluder {

    private static final Logger log = LoggerFactory.getLogger(AnnotationExcluder.class);

    /**
     * Excludes all Controllers and/or Actions that have the provided annotation applied to them from
     * triggering the provided interceptor.
     * @param interceptor The interceptor to exclude the matches for
     * @param grailsApplication The current grails application
     * @param annotation The annotation to search for
     */
    public static void excludeAnnotations(final Interceptor interceptor, final GrailsApplication grailsApplication, final Class<? extends Annotation> annotation) {
        final GrailsClass[] controllers = grailsApplication.getArtefacts("Controller");

        for (GrailsClass controller : controllers) {

            final String controllerName = controller.getLogicalPropertyName();
            final Class<?> controllerClazz = controller.getClazz();
            final Object namespace = getControllerNamespace(controllerClazz);
            final Annotation classAnnotation =  controllerClazz.getAnnotation(annotation);

            if (classAnnotation != null) {
                handleAction("*", namespace, controllerName, interceptor);
            } else{
                Arrays.stream(controllerClazz.getMethods())
                        .filter(method -> method.getAnnotation(annotation) != null && Modifier.isPublic(method.getModifiers()))
                        .forEach(methodAction -> handleAction(methodAction.getName(), namespace, controllerName, interceptor));

                Arrays.stream(controllerClazz.getDeclaredFields())
                        .filter( field -> field.getAnnotation(annotation) != null )
                        .forEach( fieldAction -> handleAction(fieldAction.getName(), namespace, controllerName, interceptor));
            }
        }
    }

    private static void handleAction(String actionName, Object namespace, String controllerName, Interceptor interceptor) {
        if(log.isDebugEnabled()) log.debug("Excluding namespace: {}, controller: {}, action: {} from interceptor: {}", namespace, controllerName, actionName, interceptor.getClass().getName());
        LinkedHashMap<String,Object> args = new LinkedHashMap<>();
        args.put("namespace", namespace);
        args.put("controller", controllerName);
        args.put("action", actionName);
        interceptor.getMatchers().forEach(matcher -> matcher.except(args));
    }

    private static Object getControllerNamespace(Class<?> controllerClazz) {
        return Arrays.stream(controllerClazz.getDeclaredFields())
                .filter(field -> Objects.equals(GrailsControllerClass.NAMESPACE_PROPERTY, field.getName()) && Modifier.isStatic(field.getModifiers()))
                .findFirst()
                .map(field -> { try { return field.get(null); } catch (IllegalAccessException e) { return null; } })
                .orElse(null);
    }
}
