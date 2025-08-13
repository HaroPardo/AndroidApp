<?php
$con = mysqli_connect("localhost", "usuario_db", "contraseña", "reportes_app");

$email = $_POST['email'];
$password = password_hash($_POST['password'], PASSWORD_DEFAULT);
$nombre = $_POST['nombre'];

$query = "INSERT INTO usuarios (email, password, nombre) VALUES ('$email', '$password', '$nombre')";

if(mysqli_query($con, $query)){
    echo "success";
} else {
    echo "error: " . mysqli_error($con);
}

mysqli_close($con);
?>