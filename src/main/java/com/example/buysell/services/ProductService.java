package com.example.buysell.services;

import com.example.buysell.models.FavoriteProduct;
import com.example.buysell.models.Image;
import com.example.buysell.models.Product;
import com.example.buysell.models.User;
import com.example.buysell.repositories.FavoriteProductRepository;
import com.example.buysell.repositories.ProductRepository;
import com.example.buysell.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FavoriteProductRepository favoriteProductRepository;

    public List<Product> listProducts(String title) {
        if (title != null) return productRepository.findByTitle(title);
        return productRepository.findAll();
    }

    public void saveProduct(Principal principal, Product product, MultipartFile file1, MultipartFile file2, MultipartFile file3) throws IOException {
        product.setUser(getUserByPrincipal(principal));
        Image image1;
        Image image2;
        Image image3;
        if (file1.getSize() != 0) {
            image1 = toImageEntity(file1);
            image1.setPreviewImage(true);
            product.addImageToProduct(image1);
        }
        if (file2.getSize() != 0) {
            image2 = toImageEntity(file2);
            product.addImageToProduct(image2);
        }
        if (file3.getSize() != 0) {
            image3 = toImageEntity(file3);
            product.addImageToProduct(image3);
        }
        log.info("Saving new Product. Title: {}; Author email: {}", product.getTitle(), product.getUser().getEmail());
        Product productFromDb = productRepository.save(product);
        productFromDb.setPreviewImageId(productFromDb.getImages().get(0).getId());
        productRepository.save(product);
    }

    public User getUserByPrincipal(Principal principal) {
        if (principal == null) return new User();
        return userRepository.findByEmail(principal.getName());
    }

    private Image toImageEntity(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }

    public void deleteProduct(User user, Long id) {
        Product product = productRepository.findById(id)
                .orElse(null);
        if (product != null) {
            if (product.getUser().getId().equals(user.getId())) {
                productRepository.delete(product);
                log.info("Product with id = {} was deleted", id);
            } else {
                log.error("User: {} haven't this product with id = {}", user.getEmail(), id);
            }
        } else {
            log.error("Product with id = {} is not found", id);
        }    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }
    public FavoriteProduct getFavProductByPreviewImageId(Long previewImageId) {
        Optional<FavoriteProduct> optionalFavoriteProduct = favoriteProductRepository.findByPreviewImageId(previewImageId);
        return optionalFavoriteProduct.orElse(null);
    }
    public void givehour(User user, Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            List<FavoriteProduct> favoriteProducts = favoriteProductRepository.getFavProductsByPreviewImageId(product.getPreviewImageId());
            if (product.getUser().getId().equals(user.getId())) {
                for (FavoriteProduct favoriteProduct : favoriteProducts) {
                    Long favoriteUserId = favoriteProduct.getUserId();
                    User favoriteUser = userRepository.findById(favoriteUserId).orElse(null);
                    if (favoriteUser != null) {
                        Integer currentHours = favoriteUser.getHours();
                        if (currentHours == null) { // Проверяем на null
                            currentHours = 0; // Устанавливаем значение по умолчанию равным 0
                        }
                        // Прибавляем цену продукта к текущему количеству часов пользователя
                        Integer price = product.getPrice(); // Предположим, что у продукта есть геттер getPrice()
                        if (price != null) { // Проверяем на null
                            favoriteUser.setHours(currentHours + price);
                            userRepository.save(favoriteUser);
                            log.info("User with id = {} received {} extra hours", favoriteUserId, price);
                        } else {
                            log.error("Price for product with id = {} is null", id);
                        }
                    } else {
                        log.error("User with id = {} not found", favoriteUserId);
                    }
                }
                // Удаляем favoriteProducts
                favoriteProductRepository.deleteAll(favoriteProducts);
                log.info("FavoriteProducts related to product with id = {} were deleted", id);
                // Удаляем продукт
                productRepository.delete(product);
                log.info("Product with id = {} was deleted", id);
            } else {
                log.error("User: {} hasn't this product with id = {}", user.getEmail(), id);
            }
        } else {
            log.error("Product with id = {} not found", id);
        }
    }
}
