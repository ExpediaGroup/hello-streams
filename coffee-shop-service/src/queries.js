import gql from "graphql-tag";

module.exports = {
    PLACE_ORDER: gql`mutation placeOrder($customerId:String!, $item:String!) {
        placeOrder(customerId: $customerId, item: $item) {
            id
            orderId
            customerId
            item
            created
        }
    }`,
    SUPPLY_BEAN: gql`mutation supplyBeans($numBeans:Int!, $actorId:String!) { supplyBeans(numBeans: $numBeans, actorId: $actorId) {
        id
        actorId
        beansSupplied
        created
    }}`,
    GET_CUSTOMER_ORDERS: gql`query getCustomerOrders($customerId:String!) {
        customer(id: $customerId) {
            orders {
                id
                item
                state
                updated
            }
        }}`,
    GET_ORDERS: gql`
        query getOrders {
            orders {
                id
                customerId
                item
                state
                updated
                created
            }
        }`,
    GET_AVAILABLE_BEANS: gql`query getAvailableBeans { availableBeans }`
}
