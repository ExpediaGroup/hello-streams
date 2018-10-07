import React from 'react';
import LoginPanel from './LoginPanel';
import beans from '../images/coffee beans.svg';

class LoginPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = { };
  }
  render() {
    return (
      <div className="App-login">
        <header className="App-header">
          <img src={beans} className="App-logo" alt="logo" />
          <h1 className="App-title">Streamable Coffee Shop</h1>
        </header>
        <LoginPanel />
      </div>
    );
  }
}

export default LoginPage;
