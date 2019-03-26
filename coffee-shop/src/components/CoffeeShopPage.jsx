import React from 'react';

import Button from '@material-ui/core/Button';
import { Link } from 'react-router-dom';
import CoffeeCounter from './CoffeeCounter';
import CustomerOrderPanel from './CustomerOrderPanel';
import BaristaPanel from './BaristaPanel';
import BeanSupplyPanel from './BeanSupplyPanel';
import './CoffeeShopPage.css'

class CoffeeShopPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {username: props.location.state.username};
  }
  render() {
    return (
      <div>
        <div className="App header">
          <div className="logout-left">Streamable Coffee Shop</div>
          <div className="logout-right">
            <span className="logout-label">Hello, <em>{this.state.username}</em> !&nbsp;</span>
            <Button component={Link} to="/" variant="outlined">Logout</Button>
          </div>
        </div>
        <div className="top">
          <div className="top-left">
            <CoffeeCounter username={this.state.username}/>
          </div>
          <div className="top-right">
            <CustomerOrderPanel username={this.state.username}/>
          </div>
        </div>
        <div className="bottom">
          <div className="bottom-left">
            <BaristaPanel />
          </div>
          <div className="bottom-right">
            <BeanSupplyPanel />
          </div>
        </div>
      </div>
    );
  }
}

export default CoffeeShopPage;
