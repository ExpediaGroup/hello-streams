import React from 'react';
import ReactDOM from 'react-dom';
import LoginPage from './LoginPage';
import { MemoryRouter } from 'react-router-dom';

it('renders without crashing', () => {
  const div = document.createElement('div');
  ReactDOM.render(
    <MemoryRouter>
      <LoginPage/>
    </MemoryRouter>
    , div);
  ReactDOM.unmountComponentAtNode(div);
});
