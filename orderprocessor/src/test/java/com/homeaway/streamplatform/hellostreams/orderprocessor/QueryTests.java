package com.homeaway.streamplatform.hellostreams.orderprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTest;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@GraphQLTest
@RunWith(SpringRunner.class)
@Slf4j
public class QueryTests {
    @Resource
    private GraphQLTestTemplate graphQLTemplate;

    @Test
    public void getAvailableBeans() throws IOException  {
        // get available beans
        GraphQLResponse response = graphQLTemplate.postForResource("getAvailableBeans.query");
        log.info("statusCode={}", response.getStatusCode());
        assertThat(response.getStatusCode().value(), is(200));
        assertThat(response.get("data.availableBeans", Integer.class), is(50));
    }
}
