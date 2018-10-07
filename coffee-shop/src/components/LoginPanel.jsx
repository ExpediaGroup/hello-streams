import React from 'react';
import barista from '../images/Barista.svg';
import {Redirect} from 'react-router-dom';
import './LoginPanel.css';

class LoginPanel extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      username: undefined,
      submitted: false
    };
    this.tmpusername = undefined;

    this.handleChange = this.handleChange.bind(this);
    this.handleLogin = this.handleLogin.bind(this);
  }

  handleChange(event) {
    this.tmpusername = event.target.value;
  }

  handleLogin(event){
    console.log('Login submitted. username = ' + this.tmpusername);
    this.setState({
      username: this.tmpusername,
      submitted: true
    });
    event.preventDefault();
  }

  render() {
    if (this.state.submitted && this.state.username) {
      return <Redirect to={{
        pathname: "/coffee-shop",
        state: { username: this.state.username }
      }} />
    }
    return (
      <div className="loginPanel">
        <div className="left"><img src={barista} className="loginPanel-image" alt="barista pic"/></div>
        <div className="right loginForm">
          <form onSubmit={this.handleLogin}>
            <div>
              <label id="loginLabel" className="label-title">Username:</label>
            </div>
            <div>
              <input type="text" htmlFor="loginLabel" required={true} placeholder="<enter username>"
                onChange={this.handleChange}/>
            </div>
            <div>
              <input type="submit" value="Login" />
            </div>
          </form>
        </div>
      </div>
    );
  }
}

export default LoginPanel;
