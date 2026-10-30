/** Mismas expresiones que usan los DTO del backend (@Pattern en RegistroRequest, CiudadanoRequest, CrearUsuarioRequest). */
export const PATRONES = {
  username: /^[a-zA-Z0-9._-]+$/,
  password: /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/,
  dni: /^\d{8}$/,
  telefono: /^\d{9}$/,
};
