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
    GET_ORDERS: gql`
        query getOrders {
            orders {
                id
                customer {
                    id
                }
                item
                state
                updated
            }
        }
    `
}