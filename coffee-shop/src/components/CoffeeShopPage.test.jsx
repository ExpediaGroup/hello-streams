import React from 'react';
import ReactDOM from 'react-dom';
import CoffeeShopPage from './CoffeeShopPage';

it('renders without crashing', () => {
  const div = document.createElement('div');
  ReactDOM.render(<CoffeeShopPage />, div);
  ReactDOM.unmountComponentAtNode(div);
});
