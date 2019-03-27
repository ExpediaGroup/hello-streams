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
import barista from '../images/Barista.svg';

import './BaristaPanel.css'

const GET_ORDERS = gql`
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

const CustomTable = withStyles(theme => ({
}))(Table);

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

class BaristaPanel extends React.Component {
  constructor(props) {
    super(props);
    this.classes = props.classes;
    this.state = {
      data: [],
    };

    this.idx = -1;
    this.createRows = this.createRows.bind(this);
    this.createRow = this.createRow.bind(this);
    this.handleListItemClick = this.handleListItemClick.bind(this);
    this.handlePlaceOrder = this.handlePlaceOrder.bind(this);
  }

  createRows(orders) {
    if (orders===undefined || orders===null) {
      return [];
    }
    this.idx = -1;
    return orders.map(x => this.createRow(x.id, x.customer.id, x.item, x.state, x.updated) );
  }

  createRow(id, customerId, item, state, updated) {
    this.idx++;
    var idx=this.idx;
    return {idx, customerId, id, item, state, updated};
  }

  handleListItemClick(event, index) {
    console.log("[INFO] " + index + " item selected");
    this.setState({ selectedIndex: index });
  }

  handlePlaceOrder(event) {
    let selectedItem = this.menuItems[this.state.selectedIndex];
    let orderPlaced = {
      sku : selectedItem.text,
      requiredBeans : selectedItem.requiredBeans
    };
    console.log("[INFO] ORDER_PLACED = "+ JSON.stringify(orderPlaced));
  }

  render() {
    return (
      <div>
        <div className="barista-header">
          <strong>Barista Alice</strong>
        </div>
        <div className="barista-grid">
          <div className="barista-left"><img src={barista} className="barista-image" alt="barista pic"/></div>
          <div className="barista-right">
            <CustomTable>
              <TableHead>
                <CustomTableRow>
                  <CustomTableCell>Order Id</CustomTableCell>
                  <CustomTableCell>Customer Id</CustomTableCell>
                  <CustomTableCell>Item</CustomTableCell>
                  <CustomTableCell>Order State</CustomTableCell>
                  <CustomTableCell>Time Updated</CustomTableCell>
                </CustomTableRow>
              </TableHead>
              <TableBody>
                <Query query={GET_ORDERS} pollInterval={750}>
                  {({ data }) => {
                    // Remove when customer is set even at the beginning (when loging is there)
                    var rows = this.createRows(data.orders);
                    return rows.map(row => (
                      <CustomTableRow className={this.classes.row} key={row.idx}>
                        <CustomTableCell component="th" scope="row">
                          {row.id.slice(-8)}
                        </CustomTableCell>
                        <CustomTableCell>{row.customerId}</CustomTableCell>
                        <CustomTableCell>{row.item}</CustomTableCell>
                        <CustomTableCell>{row.state}</CustomTableCell>
                        <CustomTableCell>{row.updated}</CustomTableCell>
                      </CustomTableRow>
                    ));
                  }}
                </Query>
              </TableBody>
            </CustomTable>
          </div>
        </div>
      </div>
    );
  }
}
BaristaPanel.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(BaristaPanel);
