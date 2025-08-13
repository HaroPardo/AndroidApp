<?php
$con = mysqli_connect("localhost", "usuario_db", "contraseña", "reportes_app");

$email = $_POST['email'];
$password = $_POST['password'];

$query = "SELECT * FROM usuarios WHERE email='$email'";
$result = mysqli_query($con, $query);

if(mysqli_num_rows($result) > 0){
    $user = mysqli_fetch_assoc($result);
    if(password_verify($password, $user['password'])){
        echo json_encode([
            'status' => 'success',
            'user_id' => $user['id'],
            'nombre' => $user['nombre']
        ]);
    } else {
        echo "invalid_password";
    }
} else {
    echo "user_not_found";
}

mysqli_close($con);
?>