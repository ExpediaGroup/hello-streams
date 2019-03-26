import React from 'react';
import LoginPage from './components/LoginPage';
import CoffeeShopPage from './components/CoffeeShopPage';
import {BrowserRouter, Route} from 'react-router-dom';
import { ApolloProvider } from "react-apollo";
import ApolloClient from "apollo-boost";

import "./App.css";

const client = new ApolloClient({
  uri: 'http://localhost:4000/',
});

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = { };
  }

  render() {
    return (
      <ApolloProvider client={client}>
        <BrowserRouter>
          <div>
            <Route exact path="/" component={LoginPage} />
            <Route path="/coffee-shop" component={CoffeeShopPage} />
          </div>
        </BrowserRouter>
      </ApolloProvider>
    );
  }
}

export default App;
