import React from 'react';
import LoginPage from './components/LoginPage';
import CoffeeShopPage from './components/CoffeeShopPage';
import {BrowserRouter, Route} from 'react-router-dom';
import "./App.css";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = { };
  }

  render() {
    return (
      <BrowserRouter>
        <div>
          <Route exact path="/" component={LoginPage} />
          <Route path="/coffee-shop" component={CoffeeShopPage} />
        </div>
      </BrowserRouter>
    );
  }
}

export default App;
