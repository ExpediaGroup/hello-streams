import 'babel-polyfill';
import {ApolloServer} from 'apollo-server';
// noinspection ES6CheckImport
import {v4 as uuidv4} from 'uuid/v4';

import {typeDefs} from './schema';
import * as queries from './queries.js';
import ApolloClient from "apollo-boost";
import fetch from "node-fetch";

global.fetch =  fetch;
const client = new ApolloClient({
  uri: "http://localhost:5000/graphql",
});

const beanClient = new ApolloClient({
  uri: "http://localhost:5100/graphql",
});

let customers = {};

function createDate() {
  return new Date().toISOString();
}

function createCustomer(customerId) {
  return {
    id: customerId,
    created: createDate(),
    updated: createDate(),
  };
}

function lookupCustomer(customerId) {
  if (!customers[customerId]) {
    customers[customerId] = createCustomer(customerId);
  }
  return customers[customerId];
}


async function getOrders() {

  let result = await client.query({
    query: queries.GET_ORDERS,
    fetchPolicy: "network-only"
  });

  let orders = result.data.orders.map(order => {
   return {
      id: order.id,
      customerId: order.customerId,
      item: order.item,
      state: order.state,
      updated: order.updated,
      created: order.created
    }
  });

 return orders;
}

async  function getAvailableBeans() {

  let result = await beanClient.query({
    query: queries.GET_AVAILABLE_BEANS,
    fetchPolicy: "network-only"
  });

  return result.data.availableBeans;
}

async function createOrderPlaced(customerId, item) {
  let vars = { customerId, item };
  let result = await client.mutate({
    mutation: queries.PLACE_ORDER,
    variables: vars,
    refetchQueries: [{
      query: queries.GET_ORDERS, // cache rules everything around me
    }],
  });

  result=result.data.placeOrder;
  return { id: result.id, orderId: result.orderId, customerId: result.customerId, item: result.item, created: result.created };
}

// hack until supply-processor in place
async function createBeansSupplied(numBeans, actorId) {
  let result = await beanClient.mutate({
    mutation: queries.SUPPLY_BEAN,
    variables: {numBeans, actorId},
    refetchQueries: [{
      query: queries.GET_AVAILABLE_BEANS, // cache rules everything around me
    }],
  });

  return result.data.supplyBeans
  // availableBeans = availableBeans + numBeans;
  // let id = uuidv4(); // uuid
  // let numBeansAdded = numBeans;
  // let created = createDate();
  // return { id, numBeansAdded, created };
}

function placeOrder(_, {customerId, item}) {
  return createOrderPlaced(customerId, item);
}

function supplyBeans(_, {numBeans, actorId}) {
  return createBeansSupplied(numBeans, actorId);
}

const resolvers = {
  CommandEvent: {
    __resolveType(obj) {
      if(obj.item) {
        return 'OrderPlaced';
      }
      if(obj.numBeansAdded) {
        return 'BeansSupplied';
      }
      return null;
    }
  },
  OrderCommandEvent: {
    __resolveType(obj) {
      if(obj.item) {
        return 'OrderPlaced';
      }
      return null;
    }
  },
  Customer: {
    async orders(customer) {
      let orders = await getOrders();
      return orders.filter( order => order.customerId===customer.id);
    }
  },
  Order: {
    customer: (order) => {
      return lookupCustomer(order.customerId);
    }
  },
  Query: {
    orders: getOrders,
    availableBeans: getAvailableBeans,
    customer(_, {id}) {
      return customers[id];
    },
  },
  Mutation: {
    placeOrder: placeOrder,
    supplyBeans: supplyBeans,
  },
};

const server = new ApolloServer({ typeDefs : [typeDefs], resolvers });

server.listen().then(({ url }) => {
  console.log(`🚀  Server ready at ${url}`);
});
