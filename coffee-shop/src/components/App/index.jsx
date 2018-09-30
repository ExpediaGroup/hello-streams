import React from 'react';
import Login from '../Login';
import beans from '../../images/coffee_beans_noa.svg';
import './index.css';

class App extends React.Component {
  render() {
    return (
      <div className="App">
        <header className="App-header">
          <img src={beans} className="App-logo" alt="logo" />
          <h1 className="App-title">Streamable Coffee Shop</h1>
        </header>
        <Login/>
      </div>
    );
  }
}

export default App;
