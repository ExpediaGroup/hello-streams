import { ApolloServer } from 'apollo-server';
import { v4 as uuidv4 } from 'uuid/v4';

import { typeDefs } from './schema';

var customers = {};
var orders = {};
var availableBeans = 50;

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

function lookupOrder(orderId) {
  if (!orders[orderId]) {
    return null;
  }
  return orders[orderId];
}

function createOrderPlaced(customerId, item) {
  const id = uuidv4();
  const order = createOrder(customerId, item);
  orders[order.id] = order;
  return {id, orderId: order.id, customerId, item, created: createDate() };
}

function createBeansSupplied(numBeansAdded) {
  const id = uuidv4();
  const created = createDate();
  return {id, numBeansAdded, created};
}

function placeOrder(_, {customerId, item}) {
  const customer = lookupCustomer(customerId);
  const event = createOrderPlaced(customerId, item);
  return event;
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
      return getOrders().filter( order => order.customerId==customer.id)
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
}

const server = new ApolloServer({ typeDefs : [typeDefs], resolvers });

server.listen().then(({ url }) => {
  console.log(`🚀  Server ready at ${url}`);
});
