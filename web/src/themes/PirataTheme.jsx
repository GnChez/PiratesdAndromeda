import { createTheme } from "@mui/material";
import PirataOne from "../assets/fonts/PirataOne-Regular.ttf";

const theme = createTheme({
  palette: {
    mode: "dark",
    primary: {
      main: "#B85C38",
      light: "#E09070",
      dark: "#8B3A18",
      contrastText: "#F7E6D8",
    },
    secondary: {
      main: "#C47A5A",
      light: "#F5CDB0",
      dark: "#5C2A1A",
      contrastText: "#0A0302",
    },
    background: {
      default: "#0A0302",
      paper: "#1A0806",
    },
    text: {
      primary: "#F5CDB0",
      secondary: "#C47A5A",
    },
    divider: "#5C2A1A",
    error: {
      main: "#E0756A",
    },
    success: {
      main: "#E09070",
    },
    warning: {
      main: "#B85C38",
    },
  },
  typography: {
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif",
    h1: {
      fontFamily: "PirataOne, serif",
      fontWeight: 700,
      letterSpacing: "0.02em",
      textTransform: "uppercase",
    },
    h2: {
      fontFamily: "PirataOne, serif",
      fontWeight: 700,
      letterSpacing: "0.02em",
      textTransform: "uppercase",
    },
    h3: {
      fontFamily: "PirataOne, serif",
      fontWeight: 700,
      letterSpacing: "0.02em",
      textTransform: "uppercase",
    },
    h4: {
      fontFamily: "PirataOne, serif",
      fontWeight: 700,
      letterSpacing: "0.02em",
      textTransform: "uppercase",
    },
    h5: {
      fontFamily: "PirataOne, serif",
      fontWeight: 700,
      letterSpacing: "0.02em",
      textTransform: "uppercase",
    },
    h6: {
      fontFamily: "PirataOne, serif",
      fontWeight: 700,
      letterSpacing: "0.02em",
      textTransform: "uppercase",
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: `
        @font-face {
          font-family: 'PirataOne';
          font-style: normal;
          font-display: swap;
          font-weight: 400;
          src: local('PirataOne'), local('PirataOne-Regular'),
               url(${PirataOne}) format('truetype');
        }
        @keyframes fadeSlideIn {
          from { opacity: 0; transform: translateY(12px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.4; }
        }
        html {
          min-height: 100%;
          background: #0A0302;
        }
        body {
          min-height: 100%;
          background: linear-gradient(180deg, #3D1008 0%, #0A0302 75%);
          color: #F5CDB0;
        }
        #root {
          min-height: 100vh;
          background: transparent;
        }
      `,
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: "#1A0806",
          borderBottom: "1px solid #5C2A1A",
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
          backgroundColor: "#1A0806",
          border: "1px solid #5C2A1A",
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 999,
          textTransform: "none",
          fontWeight: 700,
        },
        containedPrimary: {
          backgroundColor: "#B85C38",
          color: "#F7E6D8",
          "&:hover": {
            backgroundColor: "#C47A5A",
          },
        },
        outlinedPrimary: {
          borderColor: "#C47A5A",
          color: "#C47A5A",
          "&:hover": {
            borderColor: "#E09070",
            color: "#E09070",
            backgroundColor: "rgba(196,122,90,0.08)",
          },
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          "& .MuiInputLabel-root": {
            color: "#C47A5A",
          },
          "& .MuiOutlinedInput-root": {
            borderRadius: 999,
            color: "#F5CDB0",
            backgroundColor: "#1A0806",
            "& fieldset": {
              borderColor: "#5C2A1A",
            },
            "&:hover fieldset": {
              borderColor: "#B85C38",
            },
            "&.Mui-focused fieldset": {
              borderColor: "#C47A5A",
            },
          },
        },
      },
    },
  },
});
export default theme;
