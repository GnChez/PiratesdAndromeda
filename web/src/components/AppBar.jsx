import React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Container from "@mui/material/Container";
import Avatar from "@mui/material/Avatar";
import { Link as RouterLink } from "react-router-dom";

import logo from "../assets/logo/logo.png";

const navButtonStyles = {
  color: "text.secondary",
  textTransform: "none",
  fontWeight: 500,
  "&:hover": { color: "secondary.light" },
};

const AppAppBar = () => {
  return (
    <AppBar position="sticky">
      <Container maxWidth="xl">
        <Toolbar disableGutters sx={{ minHeight: 72, gap: 2 }}>
          <Box
            component={RouterLink}
            to="/"
            aria-label="Volver al inicio"
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1.2,
              flexGrow: 1,
              cursor: "pointer",
              textDecoration: "none",
              color: "inherit",
              borderRadius: 1,
              transition: "opacity 0.15s ease",
              "&:hover": { opacity: 0.85 },
              "&:focus-visible": { outline: "2px solid #E09070", outlineOffset: 2 },
            }}
          >
            <Avatar
              src={logo}
              alt="Piratas de Andromeda"
              sx={{
                width: 70,
                height: 70,
                bgcolor: "transparent",
                color: "secondary.light",
                fontWeight: 700,
                fontSize: "0.95rem",
                "& img": { objectFit: "contain" },
              }}
            >
              PA
            </Avatar>
            <Typography variant="h5" sx={{ color: "text.primary", letterSpacing: 0.6 }}>
              Piratas de Andromeda
            </Typography>
          </Box>

          <Button component={RouterLink} to="/tutorial" sx={navButtonStyles}>
            Tutorial
          </Button>
          <Button component={RouterLink} to="/qr-app" sx={navButtonStyles}>
            QR app
          </Button>
          <Button component={RouterLink} to="/about-us" sx={navButtonStyles}>
            About us
          </Button>
          <Button component={RouterLink} to="/monitorizaje" sx={navButtonStyles}>
            Monitorizaje
          </Button>
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default AppAppBar;
