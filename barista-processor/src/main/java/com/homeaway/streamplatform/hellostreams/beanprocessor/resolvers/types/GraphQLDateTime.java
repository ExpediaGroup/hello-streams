package com.homeaway.streamplatform.hellostreams.beanprocessor.resolvers.types;

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
