import React from 'react';

import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import { Query } from "react-apollo";
import gql from "graphql-tag";

import './CustomerOrderPanel.css'

const GET_CUSTOMER_ORDERS =  gql`query getCustomerOrders($customerId:String!) {
  customer(id: $customerId) {
    orders {
      id
      item
      state
      updated
    }
  }}`;

const CustomTableCell = withStyles(theme => ({
  head: {
    backgroundColor: theme.palette.common.black,
    color: theme.palette.common.white,
  },
  body: {
    fontSize: 14,
  },
}))(TableCell);

const CustomTableRow = withStyles(theme => ({
  root: {
    height: '30px',
  }
}))(TableRow);

const styles = theme => ({
  root: {
    width: '100%',
    marginTop: theme.spacing.unit * 3,
    overflowX: 'auto',
  },
  table: {
    minWidth: 700,
  },
  row: {
    '&:nth-of-type(odd)': {
      backgroundColor: theme.palette.background.default,
    },
  },
});

class CustomerOrderPanel extends React.Component {
  constructor(props) {
    super(props);
    this.classes = props.classes;
    this.state = {
      username: props.username,
      data: [],
    };

    // this.getCustomerOrders = this.getCustomerOrders.bind(this);
    this.renderRows = this.renderRows.bind(this);
    this.createOrderRow = this.createOrderRow.bind(this);
    this.createOrderRows = this.createOrderRows.bind(this);

    // this.getCustomerOrders();
  }
  /*
  getCustomerOrders() {
    client.watchQuery({
      query: GET_CUSTOMER_ORDERS,
      variables: {
        customerId: this.state.username
      },
      pollInterval: 1000
    })
    .subscribe({
      next: this.renderRows
    });
  }
  */

  renderRows({data}) {
    this.setState({data: this.createOrderRows(data.customer)});
  }

  createOrderRows(customer) {
    if (customer===undefined || customer===null || customer.orders===null) {
      return [];
    }
    this.idx = -1;
    return customer.orders.map(function(x){
      return this.createOrderRow(x.id, x.item, x.state, x.updated);
    }.bind(this));
  }

  createOrderRow(id, item, state, updated) {
    this.idx++;
    var idx=this.idx;
    return {idx, id, item, state, updated};
  }

  render() {
    var customerId = this.state.username;
    return (
      <div>
        <div className="customer-header">
          <strong>Your Orders</strong>
        </div>
        <div className="customer-grid">
          <Table>
            <TableHead>
              <CustomTableRow>
                <CustomTableCell>Order Id</CustomTableCell>
                <CustomTableCell>Item</CustomTableCell>
                <CustomTableCell>Order State</CustomTableCell>
                <CustomTableCell>Time Updated</CustomTableCell>
              </CustomTableRow>
            </TableHead>
            <TableBody>
              <Query query={GET_CUSTOMER_ORDERS} variables={{ customerId }} pollInterval={500}>
                {({ data }) => {
                  // Remove when customer is set even at the beginning (when loging is there)
                  var rows = this.createOrderRows(data.customer);
                  return rows.map(row => (
                    <CustomTableRow className={this.classes.row} key={row.idx}>
                      <CustomTableCell component="th" scope="row">
                        {row.id.slice(-8)}
                      </CustomTableCell>
                      <CustomTableCell>{row.item}</CustomTableCell>
                      <CustomTableCell>{row.state}</CustomTableCell>
                      <CustomTableCell>{row.updated}</CustomTableCell>
                    </CustomTableRow>
                  ));
                }}
              </Query>
            </TableBody>
          </Table>
        </div>
      </div>
    );
  }
}

CustomerOrderPanel.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(CustomerOrderPanel);
