import React from 'react';

import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

import './CustomerOrderDisplay.css'

const CustomTableCell = withStyles(theme => ({
  head: {
    backgroundColor: theme.palette.common.black,
    color: theme.palette.common.white,
  },
  body: {
    fontSize: 14,
  },
}))(TableCell);

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

class CustomerOrderDisplay extends React.Component {
  constructor(props) {
    super(props);
    this.classes = props.classes;
    this.state = {
      data: [],
    };

    this.createTestRow = this.createTestRow.bind(this);
    this.handleListItemClick = this.handleListItemClick.bind(this);
    this.handlePlaceOrder = this.handlePlaceOrder.bind(this);

    // TODO remove this part when this is wired with GraphQL
    this.idx = -1;
    this.state.data.push(this.createTestRow(
      "fade78f1-c7c1-47d7-952b-6cab8e386c6f",
      "Latte",
      "PLACED",
      new Date("October 10, 2018 15:00:00.123")));
    this.state.data.push(this.createTestRow(
      "b89428d9-e86c-40ff-9224-e9245a789e42",
      "Brewed Coffee",
      "PLACED",
      new Date("October 10, 2018 15:03:00.000")));
  }

  createTestRow(id, item, state, updated) {
    this.idx++;
    var idx=this.idx;
    return {idx, id, item, state, updated};
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
        <div className="customer-header">
          <strong>Your Orders</strong>
        </div>
        <div className="customer-grid">
          <Table>
            <TableHead>
              <TableRow>
                <CustomTableCell>Order Id</CustomTableCell>
                <CustomTableCell>Item</CustomTableCell>
                <CustomTableCell>Order State</CustomTableCell>
                <CustomTableCell>Time Updated</CustomTableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {this.state.data.map(row =>{
                return (
                  <TableRow className={this.classes.row} key={row.idx}>
                    <CustomTableCell component="th" scope="row">
                      {row.id.slice(-8)}
                    </CustomTableCell>
                    <CustomTableCell>{row.item}</CustomTableCell>
                    <CustomTableCell>{row.state}</CustomTableCell>
                    <CustomTableCell>{row.updated.toISOString()}</CustomTableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      </div>
    );
  }
}

CustomerOrderDisplay.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(CustomerOrderDisplay);
