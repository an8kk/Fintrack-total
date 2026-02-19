import { AuthPage } from "@refinedev/antd";

export const Login = () => {
  return (
    <AuthPage
      type="login"
      title={
        <div style={{ textAlign: "center" }}>
          <h2 style={{ color: "#2E7D32", margin: 0 }}>💰 FinTrack Admin</h2>
          <p style={{ color: "#999", fontSize: 13, margin: "4px 0 0" }}>
            Sign in with your admin account
          </p>
        </div>
      }
      formProps={{
        initialValues: { email: "", password: "" },
      }}
      hideForm={false}
      registerLink={false}
      forgotPasswordLink={false}
    />
  );
};
