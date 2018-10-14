import React from 'react';

import Button from '@material-ui/core/Button';
import beans from '../images/coffee beans.svg';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';

import './BeanSupplyPanel.css'

const styles = theme => ({
  // no customizations
});

class BeanSupplyPanel extends React.Component {
  constructor(props) {
    super(props);
    this.classes = props.classes;
    // TODO this needs to be driven by gql
    this.state = {
      beans: 50,
      numbeans: 10,
    };

    this.handleBeansSupplied = this.handleBeansSupplied.bind(this);
  }

  handleBeansSupplied(event, numbeans) {
    this.setState({
      beans: this.state.beans + numbeans,
    });
    console.log("[INFO] Added " + numbeans + " beans. beans = " + this.state.beans);
  }

  render() {
    return (
      <div>
        <div className="supply-header">
          <strong>Coffee Bean Supply</strong>
        </div>
        <div className="supply-grid">
          <div className="supply-img"><img src={beans} className="supply-image" alt="coffee beans"/></div>
          <div className="supply-label">{this.state.beans} beans</div>
          <div className="supply-action"><Button variant="outlined" onClick={(event) => this.handleBeansSupplied(event, this.state.numbeans)}>Add {this.state.numbeans} Beans</Button></div>
        </div>
      </div>
    );
  }
}

BeanSupplyPanel.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(BeanSupplyPanel);
