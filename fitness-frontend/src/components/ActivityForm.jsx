import React, {useState} from "react";
import {
    Box, FormControl, InputLabel, MenuItem, Select, TextField, FormHelperText,
} from "@mui/material";
import Button from "@mui/material/Button";
import {addActivity} from "../service/api.js";

const ActivityForm = ({onActivityAdded}) => {
    const [activity, setActivity] = useState({
        type: "RUNNING", duration: "", caloriesBurned: "", additionalMetrics: {},
    });

    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    // simple validation: duration and calories must be non-empty and >= 0
    const validate = () => {
        const durationValid = activity.duration !== "" && Number(activity.duration) >= 0;
        const caloriesValid = activity.caloriesBurned !== "" && Number(activity.caloriesBurned) >= 0;
        return durationValid && caloriesValid;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (!validate()) {
            setError("Please provide valid non-negative numbers for duration and calories.");
            return;
        }

        // Prepare payload: ensure numeric values are numbers
        const payload = {
            ...activity, duration: Number(activity.duration), caloriesBurned: Number(activity.caloriesBurned),
        };

        setSubmitting(true);
        try {
            await addActivity(payload);
            onActivityAdded?.();
            setActivity({
                type: "RUNNING", duration: "", caloriesBurned: "", additionalMetrics: {},
            });
        } catch (err) {
            console.error(err);
            setError("Failed to add activity. Please try again.");
        } finally {
            setSubmitting(false);
        }
    };

    return (<Box component="form" sx={{mb: 2}} onSubmit={handleSubmit} noValidate>
        <FormControl fullWidth sx={{mb: 2}}>
            <InputLabel id="activity-type-label">Activity Type</InputLabel>
            <Select
                labelId="activity-type-label"
                id="activity-type"
                label="Activity Type"
                value={activity.type}
                onChange={(e) => {
                    setActivity({...activity, type: e.target.value});
                }}
            >
                <MenuItem value="RUNNING">Running</MenuItem>
                <MenuItem value="WALKING">Walking</MenuItem>
                <MenuItem value="CYCLING">Cycling</MenuItem>
            </Select>

            <TextField
                fullWidth
                label="Duration (minutes)"
                type="number"
                inputProps={{min: 0}}
                value={activity.duration}
                sx={{mb: 2}}
                onChange={(e) => {
                    const v = e.target.value;
                    // keep empty string while editing; otherwise cast to Number
                    setActivity({
                        ...activity, duration: v === "" ? "" : Number(v),
                    });
                }}
                error={activity.duration !== "" && Number(activity.duration) < 0}
                helperText={activity.duration !== "" && Number(activity.duration) < 0 ? "Duration must be 0 or greater" : ""}
            />

            <TextField
                fullWidth
                label="Calories Burned"
                type="number"
                inputProps={{min: 0}}
                value={activity.caloriesBurned}
                sx={{mb: 2}}
                onChange={(e) => {
                    const v = e.target.value;
                    setActivity({
                        ...activity, caloriesBurned: v === "" ? "" : Number(v),
                    });
                }}
                error={activity.caloriesBurned !== "" && Number(activity.caloriesBurned) < 0}
                helperText={activity.caloriesBurned !== "" && Number(activity.caloriesBurned) < 0 ? "Calories must be 0 or greater" : ""}
            />

            {error && (<FormHelperText error sx={{mb: 1}}>
                {error}
            </FormHelperText>)}

            <Button type="submit" variant="contained" disabled={submitting}>
                {submitting ? "Adding..." : "Add Activity"}
            </Button>
        </FormControl>

    </Box>);
};

export default ActivityForm;
