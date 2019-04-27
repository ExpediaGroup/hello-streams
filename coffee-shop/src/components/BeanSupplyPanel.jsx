import React from 'react';

import Button from '@material-ui/core/Button';
import beans from '../images/coffee beans.svg';
import PropTypes from 'prop-types';
import { Query } from "react-apollo";
import { Mutation } from "react-apollo";
import gql from "graphql-tag";
import { withStyles } from '@material-ui/core/styles';

import './BeanSupplyPanel.css'

const GET_AVAILABLE_BEANS = gql`query getAvailableBeans { availableBeans }`;
const SUPPLY_BEANS = gql`mutation supplyBeans($numBeans:Int!, $actorId:String!) { 
  supplyBeans(numBeans: $numBeans, actorId: $actorId) {
    id
    actorId
    beansSupplied
    created
  }
}`;

const styles = theme => ({
  // no customizations
});

class BeanSupplyPanel extends React.Component {
  constructor(props) {
    super(props);
    this.classes = props.classes;
    this.state = {
      numbeans: 10,
      username: props.username
    };
  }

  render() {
    return (
      <div>
        <div className="supply-header">
          <strong>Coffee Bean Supply</strong>
        </div>
        <div className="supply-grid">
          <div className="supply-img"><img src={beans} className="supply-image" alt="coffee beans"/></div>
          <Query query={GET_AVAILABLE_BEANS} fetchPolicy={"network-only"} pollInterval={1000}>
            { ({data}) => {
                var availableBeans = data.availableBeans;
                console.log("[INFO] AVAILABLE_BEANS = "+ availableBeans);
                return (
                  <div className="supply-label">{availableBeans} beans</div>
                );
            }}
          </Query>
          <Mutation mutation={SUPPLY_BEANS} variables={ {numBeans: this.state.numbeans, actorId: this.state.username} } refetchQueries={ [{query:GET_AVAILABLE_BEANS, variables:{}, fetchPolicy:"no-cache"}] }>
              {(supplyBeans, { data }) => (
                <div className="supply-action">
                  <Button variant="outlined" onClick={(event) => {
                    event.preventDefault();
                    supplyBeans().then(res => {
                      console.log("[INFO] BEANS_SUPPLIED = "+ JSON.stringify(res.data.supplyBeans))
                    });
                  }}>Add {this.state.numbeans} Beans</Button>
                </div>
              )}
          </Mutation>
        </div>
      </div>
    );
  }
}

BeanSupplyPanel.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(BeanSupplyPanel);
