package com.homeaway.streamplatform.hellostreams.usersessionprocessor.resolvers.types;

import graphql.schema.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class GraphQLDateTime extends GraphQLScalarType {
    public GraphQLDateTime() {
        super("GraphQLDateTime", "Scalar type for DateTime", new Coercing() {
            @Override
            public Object serialize(Object o) throws CoercingSerializeException {
                return ((ZonedDateTime) o).format(DateTimeFormatter.ISO_INSTANT);
            }

            @Override
            public Object parseValue(Object o) throws CoercingParseValueException {
                return ZonedDateTime.parse((String) o, DateTimeFormatter.ISO_INSTANT);
            }

            @Override
            public Object parseLiteral(Object o) throws CoercingParseLiteralException {
                return ZonedDateTime.parse((String) o, DateTimeFormatter.ISO_INSTANT);
            }
        });
    }
}


// 2019-04-08 09:39:54.425 ERROR 71723 --- [  restartedMain] c.o.m.g.b.e.GraphQLErrorHandlerFactory   : Cannot load class schemaParser. Error creating bean with name 'schemaParser' defined in class path resource [com/oembedler/moon/graphql/boot/GraphQLJavaToolsAutoConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.coxautodev.graphql.tools.SchemaParser]: Factory method 'schemaParser' threw exception; nested exception is com.coxautodev.graphql.tools.SchemaClassScannerError: No Root resolvers for mutation type 'Mutation.java' found!  Provide one or more com.coxautodev.graphql.tools.GraphQLMutationResolver to the builder.