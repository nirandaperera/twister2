import React from "react";
import {Button, Card, Collapse, Elevation, Icon, Intent, Position, Tag, Tooltip} from "@blueprintjs/core";
import "./JobCard.css";
import JobService from "../../../services/JobService";
import {DashToaster} from "../../Dashboard";
import ClusterTag from "../../grid/clusters/ClusterTag";
import NodeTag from "../../grid/nodes/NodeTag";
import {ComputeResourceCard} from "../../grid/compute-resource/ComputeResourceCard";
import {Link} from "react-router-dom";
import {JobUtils} from "./JobUtils";

export default class JobCard extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            job: this.props.job,
            syncing: false,
            stateIntent: JobUtils.getStateIntent(this.props.job.state),
            infoOpen: false
        };
    }

    componentWillReceiveProps(nextProps, nextContext) {
        this.setState({
            job: nextProps.job,
            stateIntent: JobUtils.getStateIntent(nextProps.job.state)
        });
    }

    syncJob = () => {
        this.setState({
            syncing: true
        });

        JobService.getJobById(this.state.job.jobID).then(respone => {
            this.setState({
                job: respone.data,
                syncing: false,
                stateIntent: JobUtils.getStateIntent(respone.data.state)
            });
            DashToaster.show({
                message: "Successfully synced Job: " + this.state.job.jobID,
                intent: Intent.SUCCESS
            });
        }).catch(err => {
            DashToaster.show({
                message: "Failed to sync Job: " + this.state.job.jobID,
                intent: Intent.DANGER
            });
            this.setState({
                syncing: false
            });
        });
    };

    getStateIntent = (state) => {
        switch (state) {
            case "STARTED":
                return Intent.PRIMARY;
            case "COMPLETED":
                return Intent.SUCCESS;
            case "FAILED":
                return Intent.DANGER;
            default:
                return Intent.NONE;
        }
    };

    toggleInfo = (event) => {
        event.stopPropagation();
        this.setState({
            infoOpen: !this.state.infoOpen
        })
    };

    getTimeString = () => {
        let time = new Date(this.state.job.createdTime);
        return `${time.getMonth() + 1} / ${time.getDate()} ~ ${time.getHours()}:${time.getMinutes()}:${time.getSeconds()}`;
    };

    render() {
        return (
            <Card interactive={true} elevation={Elevation.ZERO} className="tw-table-row tw-job-row">
                <div className="tw-job-row-summary">
                    <div className="tw-job-row-icon">
                        <Icon icon="new-grid-item" iconSize={18} className="tw-row-icon"/>
                    </div>

                    <Link to={`jobs/${this.state.job.jobID}`}>
                        <div className="tw-table-row-left">
                            <div className="tw-job-row-name">
                                {this.state.job.jobName}
                            </div>
                        </div>
                    </Link>
                    <div className="tw-table-row-right">
                        {/*<NodeTag node={this.state.job.node}/>*/}
                        <Tooltip content="Job ID" position={Position.TOP}>
                            <Tag intent={Intent.NONE} icon="tag" minimal={true} style={{height: 25}}
                                 className="tw-job-row-status-tag">{this.state.job.jobID}</Tag>
                        </Tooltip>
                        <Tooltip content="Number of Workers" position={Position.TOP}>
                            <Tag intent={Intent.NONE} icon="ninja" minimal={true} style={{height: 25, width: 60}}
                                 className="tw-job-row-status-tag">{this.state.job.numberOfWorkers}</Tag>
                        </Tooltip>


                        <Tag intent={this.state.stateIntent}
                             className="tw-job-row-status-tag">{this.state.job.state}</Tag>
                        <Button rightIcon="refresh" text="" small={true} minimal={true} onClick={this.syncJob}/>
                        <Button rightIcon="caret-down" text="" small={true} minimal={true}
                                onClick={this.toggleInfo}/>

                    </div>
                </div>
                <div className="tw-job-row-info">
                    <Collapse isOpen={this.state.infoOpen} keepChildrenMounted={true}>
                        <table className=" bp3-html-table bp3-html-table-striped bp3-html-table-condensed"
                               width="100%">
                            <tbody>
                            <tr>
                                <td>
                                    Job ID
                                </td>
                                <td>
                                    {this.state.job.jobID}
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Worker Class
                                </td>
                                <td>
                                    {this.state.job.workerClass}
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Cluster
                                </td>
                                <td>
                                    <ClusterTag cluster={this.state.job.node.cluster}/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Node
                                </td>
                                <td>
                                    <NodeTag node={this.state.job.node}/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Number of Workers
                                </td>
                                <td>
                                    {this.state.job.numberOfWorkers}
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Start Time
                                </td>
                                <td>
                                    {new Date(this.state.job.createdTime).toUTCString()}
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    State
                                </td>
                                <td>
                                    <Tag minimal={true} intent={this.state.stateIntent}>{this.state.job.state}</Tag>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Compute Resources
                                </td>
                                <td>
                                    {this.state.job.computeResources.map(cr => {
                                        return <ComputeResourceCard cr={cr} key={cr.index}/>
                                    })}
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </Collapse>
                </div>
            </Card>
        )
    }
}