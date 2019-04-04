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

let customers = {};
let availableBeans = 50;

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
    query: queries.GET_ORDERS
  });

  let orders = result.data.orders.map(order => {
   return {
      id: order.id,
      customerId: order.customer.id,
      item: order.item,
      state: order.state,
      updated: order.updated
    }
  });

 return orders;
}

async function getAvailableBeans() {
  let result = await client.query({
    query: queries.GET_AVAILABLE_BEANS
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

async function createBeansSupplied(numBeans) {
  let result = await client.mutate({
    mutation: queries.SUPPLY_BEAN,
    variables: {numBeans},
    refetchQueries: [{
      query: queries.GET_AVAILABLE_BEANS, // cache rules everything around me
    }],
  });

  return result.data.supplyBeans
}

function placeOrder(_, {customerId, item}) {
  return createOrderPlaced(customerId, item);
}

function supplyBeans(_, {numBeans}) {
  return createBeansSupplied(numBeans);
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
       let orders = await getOrders()
      return orders.filter( order => order.customerId===customer.id);
    },
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
