<?php
$con = mysqli_connect("localhost", "usuario_db", "contraseña", "reportes_app");

$usuario_id = $_POST['usuario_id'];
$lugar = $_POST['lugar'];
$calificacion = $_POST['calificacion'];
$explicacion = $_POST['explicacion'];
$imagenes = $_POST['imagenes']; // Array de imágenes en base64

// Guardar imágenes en el servidor
$imagePaths = [];
foreach($imagenes as $key => $imageData){
    $imageName = time() . "_" . $key . ".jpg";
    file_put_contents("uploads/" . $imageName, base64_decode($imageData));
    $imagePaths[] = "https://tudominio.com/uploads/" . $imageName;
}

$imagePathsStr = implode(",", $imagePaths);

$query = "INSERT INTO reportes (usuario_id, lugar, calificacion, explicacion, imagenes) 
          VALUES ('$usuario_id', '$lugar', '$calificacion', '$explicacion', '$imagePathsStr')";

if(mysqli_query($con, $query)){
    echo "success";
} else {
    echo "error: " . mysqli_error($con);
}

mysqli_close($con);
?>