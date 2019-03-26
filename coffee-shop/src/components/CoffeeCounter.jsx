import React from 'react';

import Button from '@material-ui/core/Button';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import BrewedCoffeeIcon from '../images/Brewed Coffee.svg';
import EspressoIcon from '../images/Espresso.svg';
import LatteIcon from '../images/Latte.svg';
import IceCoffeeIcon from '../images/Ice Coffee.svg';
import { Mutation } from "react-apollo";
import gql from "graphql-tag";
import { withStyles } from '@material-ui/core/styles';

import './CoffeeCounter.css'

const CustomListItemIcon = withStyles(theme => ({
  root: {
    'margin-right': '32px',
  },
}))(ListItemIcon);

const PLACE_ORDER = gql`mutation placeOrder($customerId:String!, $item:String!) {
  placeOrder(customerId: $customerId, item: $item) {
    id
    orderId
    customerId
    item
    created
  }}`;

class CoffeeCounter extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      username: props.username,
      selectedIndex: undefined
    };

    this.menuItems = [
      this.createItem(BrewedCoffeeIcon, 'brewed-coffee', 'Brewed Coffee'),
      this.createItem(EspressoIcon, 'espresso', 'Espresso'),
      this.createItem(LatteIcon, 'latte', 'Latte'),
      this.createItem(IceCoffeeIcon, 'ice coffee', 'Ice Coffee')
    ];

    this.handleListItemClick = this.handleListItemClick.bind(this);
    this.handleClearOrder = this.handleClearOrder.bind(this);
  }

  createItem(component,altText,text) {
    return {component,altText,text};
  }

  handleListItemClick(event, index) {
    console.log("[INFO] " + index + " item selected");
    this.setState({ selectedIndex: index });
  }

  handleClearOrder(event) {
    this.setState({ selectedIndex: undefined });
  }

  renderListItems() {
    let items = [];
    for(let len=this.menuItems.length, i=0; i<len; ++i) {
      items.push(
        <ListItem button selected={this.state.selectedIndex === i} onClick={event => this.handleListItemClick(event,i)} key={i}>
          <CustomListItemIcon><img className="order-item" src={this.menuItems[i].component} alt={this.menuItems[i].altText}/></CustomListItemIcon>
          <ListItemText primary={this.menuItems[i].text}/>
        </ListItem>
      );
    }
    return items;
  }

  render() {
    return (
        <div>
          <div className="counter-header">
            <strong>Coffee Counter</strong>
          </div>
          <div className="counter-grid">
            <div className="conter-menu">
              <div className="counter-title">Place Order:</div>
              <List component="nav" dense={true}>
                {this.renderListItems()}
              </List>
            </div>
            <div className="action-div">
              <Mutation mutation={PLACE_ORDER}>
                {(placeOrder, { data }) => (
                  <div className="action-button">
                    <Button variant="contained" disabled={this.state.selectedIndex === undefined} color="primary" onClick={e => {
                      e.preventDefault();
                      let selectedItem = this.menuItems[this.state.selectedIndex];
                      placeOrder({ variables: {
                        customerId: this.state.username,
                        item: selectedItem.text
                      }}).then( res => {
                        console.log("[INFO] ORDER_PLACED = "+ JSON.stringify(res.data.placeOrder));
                      });
                    }}>Place Order</Button>
                  </div>
                )}
              </Mutation>
            </div>
          </div>
        </div>
    );
  }
}

export default CoffeeCounter;
