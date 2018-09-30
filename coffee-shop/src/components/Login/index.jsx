import React from 'react';
import barista from '../../images/barista_noa.svg';
import './index.css';

class Login extends React.Component {
  render() {
    return (
      <p className="login">
        <img src={barista} className="login-image" alt="barista pic"/>
        Hi Xavier!
      </p>
    );
  }
}

export default Login;
