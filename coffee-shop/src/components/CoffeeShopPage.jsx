import React from 'react';

import Button from '@material-ui/core/Button';
import { Link } from 'react-router-dom';
import CoffeeCounter from './CoffeeCounter';
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
          <div className="left">Streamable Coffee Shop</div>
          <div className="right">
            <span className="logout-label">Hello <em>{this.state.username}</em>!&nbsp;</span>
            <Button component={Link} to="/" variant="outlined">Logout</Button>
          </div>
        </div>
        <div className="top">
          <div className="top-left">
            <CoffeeCounter/>
          </div>
        </div>
        <div className="bottom">
          <div className="bottom-left">
            &nbsp;
          </div>
        </div>
      </div>
    );
  }
}

export default CoffeeShopPage;
