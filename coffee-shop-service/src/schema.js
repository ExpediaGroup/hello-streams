import { gql } from 'apollo-server';

export const typeDefs = gql`
  # declare our custom scalars
  scalar GraphQLDateTime

  "**CommandEvent** the base interface for all command events."
  interface CommandEvent {
    "The command event id"
    id: ID!
    "The timestamp of this command event"
    created: GraphQLDateTime!
  }

  "**OrderCommandEvent** is the base interface for all order command events."
  interface OrderCommandEvent {
    "The order event id"
    orderId: ID!
  }

  # "Command Events" ... e.g. often they are recording of intentions
  # they are also recordings of actions that have occurred.
  # Verbs in the past tense.

  "**OrderPlaced**: the command event that signifies the beginning of an order."
  type OrderPlaced implements OrderCommandEvent & CommandEvent {
    "The command event id"
    id: ID!
    "The order event id"
    orderId: ID!
    "The customer id who placed the order"
    customerId: String!
    "The item the customer ordered"
    item: String!
    "The timestamp of this command event"
    created: GraphQLDateTime!
  }

  "**BeansSupplied**: the command event that signifies how many beans were added to the coffee bean supply."
  type BeansSupplied implements CommandEvent {
    "The command event id"
    id: ID!
    "The actor id"
    actorId: String!
    "Number of beans added"
    beansSupplied: Int!
    "The timestamp of this command event"
    created: GraphQLDateTime!
  }

  # Traditional Domain Objects (nouns)

  "The customer domain object"
  type Customer {
    "The unique id of the customer"
    id: String!
    "The list of orders for this customer"
    orders: [Order]
    "The created datetime of this customer"
    created: GraphQLDateTime!
    "The updated datetime of this customer"
    updated: GraphQLDateTime!
  }

  "The order domain object"
  type Order {
    "The unique id of the order"
    id: ID!
    "The customer for the order"
    customer: Customer!
    "The item that was ordered"
    item: String!
    "The state of the order"
    state: String!
    "The create datetime of the order"
    created: GraphQLDateTime!
    "The updated datetime of the order"
    updated: GraphQLDateTime!
  }

  # TODO: Need to add pagination support for the order arrays
  type Query {
    "Query to retrieve all orders"
    orders: [Order!]!

    "Query to look up customer. May be null"
    customer(
      "The customerId for this customer"
      id: String!): Customer

    "Query to retrieve number of available beans"
    availableBeans: Int!
  }

  type Mutation {
    "Action for placing an order. An **OrderPlaced** command event is returned."
    placeOrder(
      "the customerId placing this order"
      customerId: String!,
      "the item that is being placed for this order"
      item: String!) : OrderPlaced!

    "Action for supplying beans.  A **BeanSupplied** command event is returned."
    supplyBeans(
      "the number of beans to supply"
      numBeans: Int!
      "the actorId"
      actorId: String!
    ) : BeansSupplied!
  }
`;
