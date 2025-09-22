import React, {useState} from "react";
import {Box, FormControl, InputLabel, MenuItem, Select, TextField} from "@mui/material";
import Button from "@mui/material/Button";

const ActivityForm = ({onActivityAdded}) => {
    const [activity, setActivity] = useState({
        type: "RUNNING", duration: '', caloriesBurned: '', additionalMetrics: {}
    });

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // await addActivity(activity);
            onActivityAdded();
            setActivity({
                type: "RUNNING", duration: '', caloriesBurned: '', additionalMetrics: {}
            })
        } catch (error) {
            console.log(error);
        }
    }

    return (<Box component="form" sx={{mb: 2}} onSubmit={handleSubmit}>
        <FormControl fullWidth sx={{mb: 2}}>
            <InputLabel id="demo-simple-label">
                Activity Type
            </InputLabel>
            <Select
                value={activity.type}
                onChange={(e) => {
                    setActivity({...activity, type: e.target.value});
                }}>
                <MenuItem value="RUNNING">Running</MenuItem>
                <MenuItem value="WALKING">Walking</MenuItem>
                <MenuItem value="CYCLING">Cycling</MenuItem>
            </Select>
            <TextField fullWidth label="Duration" type='number' value={activity.duration} sx={{mb: 2}}
                       onChange={(e) => {
                           setActivity({...activity, type: e.target.value})
                       }}></TextField>
            <TextField fullWidth label="Calories Burned" type='number' value={activity.caloriesBurned} sx={{mb: 2}}
                       onChange={(e) => {
                           setActivity({...activity, type: e.target.value})
                       }}></TextField>
            <Button type='submit' variant='contained'>Add Activity</Button>
        </FormControl>
    </Box>)
}

export default ActivityForm;