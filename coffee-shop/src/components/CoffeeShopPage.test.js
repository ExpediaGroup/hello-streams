import React from 'react';
import ReactDOM from 'react-dom';
import CoffeeShopPage from './CoffeeShopPage';
import { MemoryRouter } from 'react-router-dom';

it('renders without crashing', () => {
  const div = document.createElement('div');
  ReactDOM.render(
    <MemoryRouter>
      <CoffeeShopPage location={{state: {username: 'Bob'}}}/>
    </MemoryRouter>
    , div);
  ReactDOM.unmountComponentAtNode(div);
});
