import 'babel-polyfill';
import {ApolloServer} from 'apollo-server';
// noinspection ES6CheckImport
import {v4 as uuidv4} from 'uuid/v4';

import {typeDefs} from './schema';

import ApolloClient from "apollo-boost";
import fetch from "node-fetch";
import gql from "graphql-tag";

// setup global fetcher
global.fetch = fetch;
const client = new ApolloClient({
  uri: "http://localhost:5000/graphql",
});

let customers = {};
let orders = {};
let availableBeans = 50;

const PLACE_ORDER = gql`mutation placeOrder($customerId:String!, $item:String!) {
  placeOrder(customerId: $customerId, item: $item) {
    id
    orderId
    customerId
    item
    created
  }
}`;

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

function getOrders() {
  return Object.values(orders);
}

function createOrder(customerId, item) {
  const id = uuidv4();
  return { id, customerId, item, state: "PLACED",
  created: createDate(), updated: createDate() };
}

async function createOrderPlaced(customerId, item) {
  let vars = { customerId, item };
  let result = await client.mutate({
    mutation: PLACE_ORDER,
    variables: vars
  });
  console.log("result="+JSON.stringify(result));

  result=result.data.placeOrder;
  const id = result.id;
  const order = createOrder(result.customerId, result.item);
  return { id: result.id, orderId: result.orderId, customerId: result.customerId, item: result.item, created: result.created };
}

function createBeansSupplied(numBeansAdded) {
  const id = uuidv4();
  const created = createDate();
  return {id, numBeansAdded, created};
}

function placeOrder(_, {customerId, item}) {
  const customer = lookupCustomer(customerId);
  if (customer === undefined) {
    return undefined;
  }
  return createOrderPlaced(customerId, item);
}

function supplyBeans(_, {numBeans}) {
  const event = createBeansSupplied(numBeans);
  availableBeans += numBeans;
  return event;
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
  Order: {
    customer(order) {
      return lookupCustomer(order.customerId)
    },
  },
  Customer: {
    orders(customer) {
      return getOrders().filter( order => order.customerId===customer.id)
    },
  },
  Query: {
    orders: getOrders,
    customer(_, {id}) {
      return customers[id];
    },
    availableBeans() {
      return availableBeans;
    }
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
