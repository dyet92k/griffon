/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lombok.eclipse.handlers;

import griffon.transform.EventPublisher;
import lombok.core.AnnotationValues;
import lombok.core.handlers.EventPublisherHandler;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseType;
import org.codehaus.griffon.compile.core.EventPublisherConstants;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.kordamp.jipsy.ServiceProviderFor;

import static lombok.core.util.ErrorMessages.canBeUsedOnClassAndEnumOnly;
import static lombok.eclipse.handlers.EclipseHandlerUtil.createAnnotation;

/**
 * @author Andres Almiray
 */
@ServiceProviderFor(EclipseAnnotationHandler.class)
public class HandleEventPublisher extends EclipseAnnotationHandler<EventPublisher> {
    private final EclipseEventPublisherHandler handler = new EclipseEventPublisherHandler();

    @Override
    public void handle(AnnotationValues<EventPublisher> annotation, Annotation source, EclipseNode annotationNode) {
        EclipseType type = EclipseType.typeOf(annotationNode, source);
        if (type.isAnnotation() || type.isInterface()) {
            annotationNode.addError(canBeUsedOnClassAndEnumOnly(EventPublisher.class));
            return;
        }

        EclipseNode eventPublisherNode = type.getAnnotation(EventPublisher.class);
        AnnotationValues<EventPublisher> eventPublisherAnnotation = createAnnotation(EventPublisher.class, eventPublisherNode);

        EclipseUtil.addInterface(type.get(), EventPublisherConstants.EVENT_PUBLISHER_TYPE, source);
        handler.addEventPublisherSupport(type, eventPublisherAnnotation.getInstance());
        type.editor().rebuild();
    }

    private static class EclipseEventPublisherHandler extends EventPublisherHandler<EclipseType> {
    }
}
