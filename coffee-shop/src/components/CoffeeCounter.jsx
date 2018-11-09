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
import { withStyles } from '@material-ui/core/styles';

import './CoffeeCounter.css'

const CustomListItemIcon = withStyles(theme => ({
  root: {
    'margin-right': '32px',
  },
}))(ListItemIcon);

class CoffeeCounter extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      username: props.username,
      selectedIndex: undefined
    };

    this.menuItems = [
      this.createItem(BrewedCoffeeIcon, 'brewed-coffee', 'Brewed Coffee', 5),
      this.createItem(EspressoIcon, 'espresso', 'Espresso', 8),
      this.createItem(LatteIcon, 'latte', 'Latte', 7),
      this.createItem(IceCoffeeIcon, 'ice coffee', 'Ice Coffee', 7)
    ];

    this.handleListItemClick = this.handleListItemClick.bind(this);
    this.handlePlaceOrder = this.handlePlaceOrder.bind(this);
    this.handleClearOrder = this.handleClearOrder.bind(this);
  }

  createItem(component,altText,text,requiredBeans) {
    return {component,altText,text,requiredBeans};
  }

  handleListItemClick(event, index) {
    console.log("[INFO] " + index + " item selected");
    this.setState({ selectedIndex: index });
  }

  uuidv4() {
    return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
      // eslint-disable-next-line no-mixed-operators
      (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
  }

  createOrder(id, customerId, sku, requiredBeans) {
    return {id, customerId, sku, requiredBeans};
  }

  handlePlaceOrder(event) {
    let selectedItem = this.menuItems[this.state.selectedIndex];
    let orderPlaced = this.createOrder(this.uuidv4(), this.state.username, selectedItem.text, selectedItem.requiredBeans);
    console.log("[INFO] ORDER_PLACED = "+ JSON.stringify(orderPlaced));
    this.setState( {selectedIndex:undefined} );
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
              <div className="action-button"><Button variant="contained" disabled={this.state.selectedIndex === undefined} color="primary" onClick={this.handlePlaceOrder}>Place Order</Button></div>
            </div>
          </div>
        </div>
    );
  }
}

export default CoffeeCounter;
